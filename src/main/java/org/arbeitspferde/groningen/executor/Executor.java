/* Copyright 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arbeitspferde.groningen.executor;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.PipelineStageInfo;
import org.arbeitspferde.groningen.PipelineStageState;
import org.arbeitspferde.groningen.PipelineSynchronizer;
import org.arbeitspferde.groningen.common.SubjectSettingsFileManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.extractor.CollectionLogAddressor;
import org.arbeitspferde.groningen.extractor.Extractor;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.subject.HealthQuerier;
import org.arbeitspferde.groningen.subject.ServingAddressGenerator;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.subject.SubjectInterrogationException;
import org.arbeitspferde.groningen.subject.SubjectInterrogator;
import org.arbeitspferde.groningen.subject.SubjectManipulator;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.FileFactory;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.utility.PermanentFailure;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Executor starts experiments and monitors them in production. If an
 * experimental subject is restarting too often then it is reset to the default JVM
 * settings.
 *
 * The Executor uses an {@link Experiment} to get a list of subjects to restart and
 * then uses {@link SubjectManipulator} on the subjects within the population to restart them.
 * They must be restarted so that they update their JVM settings based on the output of
 * the {@link Generator} to start the next round of experiments.
 *
 * Once the experiment is started, the Executor polls subjects iteratively to
 * determine their uptime by scraping metrics. This is used to determine if a subject
 * has restarted. If it restarts too often it will be reset to default JVM
 * settings.
 *
 * The Executor shares data on subject restarts it records while monitoring the
 * subjects. This data is indexed in an InMemoryCache by subjectId. The {@link Validator}
 * uses this data to determine if a subject is valid.
 */
@PipelineIterationScoped
public class Executor extends ProfilingRunnable {
  /** Logger for this class */
  private static final Logger log = Logger.getLogger(Executor.class.getCanonicalName());

  @Inject
  @NamedConfigParam("executor_wait_for_one_subject_restart_ms")
  private int executorWaitForOneSubjectRestartMs =
      GroningenParams.getDefaultInstance().getExecutorWaitForOneSubjectRestartMs();

  @Inject
  @NamedConfigParam("executor_sleep_btwn_polling_metrics_ms")
  private int executorSleepBtwnPollingMetricsMs =
      GroningenParams.getDefaultInstance().getExecutorSleepBtwnPollingMetricsMs();

  @Inject
  @NamedConfigParam("extractor_number_of_extractor_threads")
  private int extractorNumberOfExtractorThreads =
      GroningenParams.getDefaultInstance().getExtractorNumberOfExtractorThreads();

  @Inject
  @NamedConfigParam("maximum_inflight_subject_restart_count")
  private int subjectRestartRateLimit =
      GroningenParams.getDefaultInstance().getMaximumInflightSubjectRestartCount();

  @Inject
  @NamedConfigParam("duration")
  private int experimentDuration =
      GroningenParams.getDefaultInstance().getDuration();

  /** The Experiment Database */
  private final ExperimentDb experimentDb;
  private final SubjectManipulator manipulator;
  private final HealthQuerier healthQuerier;
  private final SubjectInterrogator subjectInterrogator;
  private final PipelineSynchronizer pipelineSynchronizer;
  private final SubjectSettingsFileManager subjectSettingsFileManager;
  private final MetricExporter metricExporter;
  private final FileFactory fileFactory;
  private final ServingAddressGenerator servingAddressBuilder;
  private final CollectionLogAddressor addressor;
  private final PipelineStageInfo pipelineStageInfo;

  private long whenExperimentStarted;
  private final Clock clock;

  private ExecutorService extractorService;
  private ExecutorService executorService;
  private List<SubjectStateBridge> subjects;
  private Boolean steadyState = false;

  /** Counts the number of subjects removed from Experiments for exceptional reasons */
  private AtomicLong removeSubjectFromExperimentCount = new AtomicLong(0);

  /** Counts the number of subjects missing associated UNIX processes */
  private AtomicLong subjectMissingAssociatedProcess = new AtomicLong(0);

  /** Counts the number of successfully executed subjects */
  private AtomicLong successfullyExecutedSubjects = new AtomicLong(0);

  /** Counts the number of times subjects have restarted across all experiments */
  private AtomicLong restartedSubjectCount = new AtomicLong(0);

  /** The current population size of actively running subjects in the experiment */
  private AtomicLong currentPopulationSize = new AtomicLong(0);

  private Experiment lastExperiment = null;

  /** The maximum per subject warm up time within the current experiment */
  private long maxWarmup = 0;

  /** Stores current and end times of experiment */
  private AtomicLong now = new AtomicLong();
  private AtomicLong end = new AtomicLong();

  /** Stores the time left until end of current experiment */
  private TimeLeft timeLeft = new TimeLeft(now, end);

  @Inject
  public Executor(final Clock clock, final MonitorGroningen monitor, final ExperimentDb e,
                  final SubjectManipulator manipulator, final HealthQuerier healthQuerier,
                  final SubjectInterrogator subjectInterrogator,
                  final PipelineSynchronizer pipelineSynchronizer,
                  final SubjectSettingsFileManager subjectSettingsFileManager,
                  final MetricExporter metricExporter, final FileFactory fileFactory,
                  final ServingAddressGenerator servingAddressBuilder,
                  final CollectionLogAddressor addressor,
                  final PipelineStageInfo pipelineStageInfo) {
    super(clock, monitor);

    this.clock = clock;
    experimentDb = e;
    this.manipulator = manipulator;

    this.healthQuerier = healthQuerier;
    this.subjectInterrogator = subjectInterrogator;
    this.pipelineSynchronizer = pipelineSynchronizer;
    this.subjectSettingsFileManager = subjectSettingsFileManager;
    this.metricExporter = metricExporter;
    this.fileFactory = fileFactory;
    this.servingAddressBuilder = servingAddressBuilder;
    this.addressor = addressor;
    this.pipelineStageInfo = pipelineStageInfo;
  }

  /**
   * Sleeps the specified number of milliseconds, or until an
   * {@link InterruptedException} is caught;
   *
   * @param millis
   */
  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      return;
    }
  }


  /**
   * Synchronously restart all of the subjects in a given subject group. When this
   * method returns all of the subjects have restarted
   *
   * @param subjectGroup The subject group whose subjects should be restarted
   * @param numTries How many times to try this operation before throwing an
   *        exception. Must be positive
   * @throws PermanentFailure when restarting fails
   */
  private void restartGroup(SubjectGroup subjectGroup, int numTries) throws PermanentFailure {
    Preconditions.checkArgument(numTries > 0, "numTries must be positive.");

    long switchRateLimit = subjectRestartRateLimit * 1000L;
    long maxWaitMillis = -1;
    /** Number of milliseconds to backoff, which doubles each time. */
    int backoff = 1000;

    for (int i = 0; i < numTries; i++) {
      try {
        manipulator.restartGroup(subjectGroup, switchRateLimit, maxWaitMillis);
        return;
      } catch (final Exception e) {
        log.log(Level.SEVERE,
            String.format("Exception during subject group (%s) restart after %s tries.",
                subjectGroup, numTries), e);
        sleep(backoff);
        backoff *= 2;
      }
    }
    throw new PermanentFailure(String.format(
        "Unable to restart subject group after %s tries: %s", subjectGroup, numTries));
  }

  /**
   * Synchronously restart a given subject. When this method returns the subject
   * will have restarted
   *
   * @param subject The subject to be restarted
   * @param numTries How many times to try this operation before throwing an
   *        exception. Must be positive
   * @throws PermanentFailure when restarting fails
   */
  private void restartSubject(Subject subject, int numTries) throws PermanentFailure {
    Preconditions.checkArgument(numTries > 0, "numTries must be positive.");

    /** Number of milliseconds to backoff, which doubles each time. */
    int backoff = 1000;
    for (int i = 0; i < numTries; i++) {
      try {
        manipulator.restartIndividual(subject, executorWaitForOneSubjectRestartMs);
        return;
      } catch (final Exception e) {
        log.log(Level.SEVERE,
            String.format("Exception during subject (%s) restart after %s tries.",
                subject, numTries), e);
        sleep(backoff);
        backoff *= 2;
      }
    }
    throw new PermanentFailure(String.format(
        "Unable to restart subject after %s tries: %s", subject, numTries));
  }

  /**
   * Restart all of the subject groups listed in the config
   */
  private void restartAllGroups(GroningenConfig config) {
    for (ClusterConfig cluster : config.getClusterConfigs()) {
      String clusterName = cluster.getName();
      for (SubjectGroupConfig subjectGroup : cluster.getSubjectGroupConfigs()) {
        String groupName = subjectGroup.getName();
        String userName = subjectGroup.getUser();
        // Restart the whole group; trying 6 times before logging an error and trying again.
        SubjectGroup group =
            new SubjectGroup(clusterName, groupName, userName, subjectGroup, servingAddressBuilder);
        boolean notDone = true;
        while(notDone) {
          try {
            group.initialize(manipulator);
            restartGroup(group, 6);
            notDone = false;
          } catch (Exception e) {
            log.log(Level.SEVERE, "Could not restart all subjects.", e);
          }
        }
      }
    }
  }

  /**
   * Scrape metrics in order to get the last restart time for a subject.
   *
   * <p>We sleep after each retry for t(i) = 1000 * i * i milliseconds
   *
   * @param subject The subject whose last restart time should be queried
   * @return The last restart time expressed as a number of seconds since the
   *         epoch
   * @throws NumberFormatException if the metrics data is incorrect
   */
  public long queryLastRestartTime(Subject subject) {
    String rawInput = subjectInterrogator.getLastSubjectRestartTime(subject);
    return Long.parseLong(rawInput);
  }

  /**
   * Returns the command-line string that was used in the given subject.
   *
   * <p>We sleep after each retry for t(i) = 1000ms * i * i milliseconds
   *
   * @param subject Subject whose command-line string needs to be extracted.
   * @return Command-line string.
   */
  public String queryCommandLine(Subject subject) {
    String commandLine = subjectInterrogator.getCommandLine(subject);
    checkCommandLine(commandLine);
    return commandLine;
  }

  /**
   * Generate warnings about any missing or unusual command line flags
  *
   * We check for these problems and possibly produce a warning:
   *   1. Missing -XX:+PrintGCApplicationStoppedTime
   */
  private void checkCommandLine(String commandLine) {
    String warning;

    if (commandLine != null) {
      if (commandLine.indexOf("-XX:+PrintGCApplicationStoppedTime") < 0) {
        warning = "Groningen requires that you set the JVM flag " +
            "-XX:+PrintGCApplicationStoppedTime on your experimental " +
            "subjects so that it can determine the subject's pause times";
        log.warning(warning);
        monitor.addWarning(warning);
      }
    }
  }
  
  /**
   * Provide the remaining time within the run of the experiment.
   *
   * This does not include restart or wait times and is only valid once the experiment has
   * entered the steady state.
   *
   * @return time in secs remaining in the experiment, -1 if the request was made outside the
   *    valid window.
   */
  public long getRemainingDurationSeconds() {
    // verify we are in a state that can have a duration
    if (pipelineStageInfo.getImmutableValueCopy().state != PipelineStageState.EXECUTOR_MAIN) {
      return -1;
    }

    long remainingMillisecs = whenExperimentStarted + maxWarmup +
        (1000 * 60 * (long) experimentDuration) - clock.now().getMillis();

    return remainingMillisecs >= 0 ? remainingMillisecs / 1000L : 0L;
  }

  @Override
  public void profiledRun(GroningenConfig config) throws RuntimeException {
    preSteps(config);
    steadyState(config);
    postSteps(config);
    log.info("Experiment execution complete");
  }

  /**
   * The steps performed to intialize and otherwise setup before the experiment can run. These are
   * the steps we perform:
   *   1. Restart all of the subjects of all of the group that we control
   *   2. Get the experiment we're going to execute
   *   3. Create a list of Subjects we are going to include in the experiment
   *   4. Query all of the subjects to store their restart times for later comparison
   *   5. Create a new fixed thread pool to contain the Extractor threads we'll use for log analysis
   *
   * The experiment duration and warmup times are summed and we start the experiment timer only
   * after restarting all the subjects.
   */
  private void preSteps(GroningenConfig config) {
    log.info("Executor is performing its pre steps");

    // monitor the running time
    monitor.monitorObject(timeLeft, "Time left until end of current experiment");

    // Get the experiment we're going to execute
    lastExperiment = experimentDb.getLastExperiment();
    if (lastExperiment == null) {
      // TODO(team): should this be a warning or an error - how can this occur in a valid state?
      // how do we get out of this state to run the experiment?
      log.warning("Experiments do not exist. Skipping Executor stage.");
    } else {
      // Restart all of the subjects of all of the group.
      pipelineStageInfo.set(PipelineStageState.INITIAL_TASK_RESTART);
      restartAllGroups(config);

      // Create a list of Subjects we are going to include in the experiment. This may be less than
      // the size of the list of subjects generated by the Hypothesizer into this experiment because
      // the Generator might not have been able to verify that all the required subjects and groups
      // are actually available in production.
      subjects = Collections.synchronizedList(new ArrayList<SubjectStateBridge>());
      for (SubjectStateBridge subject : lastExperiment.getSubjects()) {
        if (subject.getAssociatedSubject() == null) {
          // For some reason, the Generator might not have given us all the subjects that we require
          subjectMissingAssociatedProcess.incrementAndGet();
          log.warning(String.format(
              "Executor: Subject %s missing associated UNIX process", subject.getIdOfObject()));
        } else {
          subjects.add(subject);

          // Find the max warmup time used to offset the experiment start time
          if (maxWarmup < subject.getWarmupTimeoutMillis()) {
            maxWarmup = subject.getWarmupTimeoutMillis();
          }

          // Set the timestamp used to track the per subject warmup time when in the NEW state
          subject.setTimestamp(clock.now().getMillis());

          // Indicate this subject was started in the experiment in production
          subject.getSubjectRestart().subjectStarted();
        }
      }

      if (pipelineSynchronizer != null) {
        pipelineSynchronizer.initialSubjectRestartCompleteHook();
      }

      if (subjects.size() > 0) {
        // The warmup must be added to the start time to provide all of the subjects time to warm up
        whenExperimentStarted = clock.now().getMillis();

        // Create the thread services we'll use for experimental probing and extraction
        extractorService = Executors.newFixedThreadPool(extractorNumberOfExtractorThreads);
        executorService = Executors.newFixedThreadPool(
            config.getParamBlock().getNumberOfExecutorThreads());

        log.info(String.format("The initial population size is: %s", subjects.size()));

        steadyState = true;
      } else {
        log.warning("Skipping experiment because there are no subjects.");
      }
    }
  }

  /** The Executor's steady state, during which it runs the experiment */
  private void steadyState(GroningenConfig config) {
    if (!steadyState) {
      log.warning("Unable to enter steady state because we're uninitialized");
    } else {
      log.info("Executor is entering its steady state");
      pipelineStageInfo.set(PipelineStageState.EXECUTOR_MAIN);

      do {
        currentPopulationSize.set(subjects.size());
        for (SubjectStateBridge subject : subjects) {
          executorService.execute(new ExecutorStateMachine(config, subject));
        }
        log.info(String.format("Main executor thread is sleeping between polling rounds for %s" +
            " ms while the probing threads do their stuff", executorSleepBtwnPollingMetricsMs));
        sleep(executorSleepBtwnPollingMetricsMs);
      } while (!experimentIsDone(config, subjects));
      log.info("Leaving steadyState as the experiment is done.");
    }
  }

  /**
   * These steps are performed to tear down and otherwise cleanup after running the experiment:
   *   1. Restart subjects with default JVM settings and run Extractor on them
   *   2. Shutdown the extractorService because we will not be sending it any more work
   *   3. Wait for all of the Extractors to complete
   *   4. Clear associated metrics
   */
  private void postSteps(GroningenConfig config) {
    if (!steadyState) {
      log.warning("Unable to perform post steps because we're unitialized");
    } else {
      log.info("Executor is performing its post steps");
      log.info(String.format("The final population size is %s.", subjects.size()));

     // Restart subjects with default JVM settings and run Extractor on them
      pipelineStageInfo.set(PipelineStageState.REMOVING_EXPERIMENTAL_ARGUMENTS);
      for (SubjectStateBridge subject : subjects) {
        // Clear the JVM settings protobuf to cause subjects to restart with default JVM settings
        subjectSettingsFileManager.delete(subject.getAssociatedSubject().getExpSettingsFile());
      }
      
      pipelineStageInfo.set(PipelineStageState.FINAL_TASK_RESTART);    
      restartAllGroups(config);
      for (SubjectStateBridge subject : subjects) {
        log.info(String.format("Running extractor thread on %s.", subject.getHumanIdentifier()));
        extractorService.execute(
            new Extractor(config, subject, metricExporter, fileFactory, addressor));
        successfullyExecutedSubjects.incrementAndGet();
      }

      // Tell the services that we will not be sending them any more work
      extractorService.shutdown();
      executorService.shutdown();

      // Wait for all of the services to complete
      try {
        log.info("Executor is attempting to exit. "
            + "We will wait up to 5 minutes for all Extractor threads to complete.");
        if (!extractorService.awaitTermination(5, TimeUnit.MINUTES)) {
          log.warning(
              "Executor waited 5 minutes for all Extractor threads to complete, but they didn't.");
        }
        log.info("Executor is attempting to exit. "
            + "We will wait up to 5 minutes for all Executor threads to complete.");
        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
          log.warning(
              "Executor waited 5 minutes for all Executor threads to complete, but they didn't.");
        }
      } catch (InterruptedException e) {
        log.severe(e.toString());
      }

      // Reset metrics.
      removeSubjectFromExperimentCount.set(0);
      subjectMissingAssociatedProcess.set(0);
      successfullyExecutedSubjects.set(0);
      restartedSubjectCount.set(0);
      currentPopulationSize.set(0);

      // Reset properties
      lastExperiment = null;
      subjects = null;
      executorService = null;
      extractorService = null;
      steadyState = false;
      whenExperimentStarted = 0;
      maxWarmup = 0;
    }

    // stop monitoring timeLeft
    monitor.stopMonitoringObject(timeLeft);
  }

  /** Returns true when the experiment duration has elapsed and false otherwise */
  private boolean experimentIsDone(GroningenConfig config, List<SubjectStateBridge> subjects) {
    if (pipelineSynchronizer != null) {
      if (pipelineSynchronizer.shouldFinalizeExperiment()) {
        return true;
      }
    }

    now.set(clock.now().getMillis());
    end.set(whenExperimentStarted + maxWarmup + (1000 * 60 * (long) experimentDuration));

    if (now.get() >= end.get()) {
      log.info("Experiment duration expired. Ending experiment.  Time elapsed: "
          + (now.get() - whenExperimentStarted) + " milliseconds.");
      return true;
    }

    if (subjects.size() == 0) {
      log.warning("Experiment ended prematurely because all of the subjects are dead.");
      return true;
    }

    return false;
  }

  /**
   * Removes a {@link SubjectStateBridge} from the experiment by resetting its subject's JVM
   * parameters to default values and restarting the subject.
   *
   * @param subject The {@link SubjectStateBridge} to be removed.
   */
  private void removeSubjectFromExperiment(SubjectStateBridge subject) {
    subjects.remove(subject);
    subjectSettingsFileManager.delete(subject.getAssociatedSubject().getExpSettingsFile());
    try {
      restartSubject(subject.getAssociatedSubject(), 4);
    } catch (Exception e) {
      log.severe("Removing a subject, even though restart failed." + e.toString());
    }
    removeSubjectFromExperimentCount.incrementAndGet();
    subject.removeFromExperiment();
    log.info(String.format("Removing subject from experiment %s.", subject.getHumanIdentifier()));
  }

  /** This is the thread safe state machine that probes subjects during experiment execution */
  class ExecutorStateMachine implements Runnable {
    private final GroningenConfig config;
    private final SubjectStateBridge subject;

    public ExecutorStateMachine(GroningenConfig config, SubjectStateBridge subject) {
      this.config = config;
      this.subject = subject;
    }

    @Override
    public void run() {
      try {
        // NEW subject processing
        if (subject.getState() == SubjectStateBridge.State.NEW) {
          steadyStateNew();
        }
        // HEALTHY subject processing
        if (subject.getState() == SubjectStateBridge.State.HEALTHY) {
          steadyStateHealthy();
        }
      } catch (final Exception e) {
        log.log(Level.WARNING,
            String.format("[MARKING UNHEALTHY]: %s.", subject.getHumanIdentifier(), e));
        subject.setState(SubjectStateBridge.State.UNHEALTHY);
      }
      // UNHEALTHY subject processing
      if (subject.getState() == SubjectStateBridge.State.UNHEALTHY) {
        steadyStateUnhealthy();
      }
      // DEAD subject processing
      if (subject.getState() == SubjectStateBridge.State.DEAD) {
        steadyStateDead();
      }
    }

    private void steadyStateHealthy()
        throws NumberFormatException, SubjectInterrogationException {
      log.info(String.format("Probing healthy subject %s.", subject.getHumanIdentifier()));

      long lastRestartTime = queryLastRestartTime(subject.getAssociatedSubject());
      long restartTime = subject.getSubjectRestart().getLastRestartTime();
      if (lastRestartTime > restartTime) {
        subject.getSubjectRestart().setLastRestartTime(lastRestartTime);
        subject.getSubjectRestart().anotherRestart();
        restartedSubjectCount.incrementAndGet();
        if (subject.getSubjectRestart().restartThresholdCrossed(config)) {
          log.warning(String.format("[MARKING UNHEALTHY]: %s restart threshold crossed.  %s > %s.",
              subject.getHumanIdentifier(), lastRestartTime, restartTime));
          subject.setState(SubjectStateBridge.State.UNHEALTHY);
          subject.setTimestamp(clock.now().getMillis());
        } else {
          log.info(String.format("Running extractor thread on %s.", subject.getHumanIdentifier()));
          extractorService.execute(
              new Extractor(config, subject, metricExporter, fileFactory, addressor));
        }
        subject.addCommandLineString(queryCommandLine(subject.getAssociatedSubject()));
      }
    }

    private void steadyStateUnhealthy() {
      log.info(String.format("Probing unhealthy subject %s.", subject.getHumanIdentifier()));

      if (!warmupTimeHasExpired()) {
        if (healthQuerier.blockUntilHealthy(subject.getAssociatedSubject())) {
          log.info(String.format(
              "[MARKING HEALTHY]: Health check passed %s.", subject.getHumanIdentifier()));
          subject.setState(SubjectStateBridge.State.HEALTHY);
        }
      } else {
        subject.setState(SubjectStateBridge.State.DEAD);
      }
    }

    private void steadyStateDead() {
      log.info(String.format("Removing dead subject %s.", subject.getHumanIdentifier()));
      removeSubjectFromExperiment(subject);
    }

    private void steadyStateNew() throws NumberFormatException, SubjectInterrogationException {
      final String subjectIdentifier = subject.getHumanIdentifier();

      log.info(String.format("Probing new subject %s.", subjectIdentifier));

      // This is very much like what we do to unhealthy subjects, except for the part about
      // setting the command line and the restart time, which is important so we don't mistakenly
      // process invalid log files or miss a subject's command line for validation later.
      if (!warmupTimeHasExpired()) {
        if (healthQuerier.blockUntilHealthy(subject.getAssociatedSubject())) {
          log.info(String.format("Subject is warmed up %s.", subjectIdentifier));
          subject.setState(SubjectStateBridge.State.HEALTHY);
          subject.addCommandLineString(queryCommandLine(subject.getAssociatedSubject()));
          subject.getSubjectRestart().setLastRestartTime(queryLastRestartTime(
              subject.getAssociatedSubject()));
        }
      } else {
        subject.setState(SubjectStateBridge.State.DEAD);
      }
    }

    private boolean warmupTimeHasExpired() {
      return (clock.now().getMillis() - subject.getTimestamp())
          > (subject.getWarmupTimeoutMillis());
    }
  }


  /** A class to handle storage and display of time left */
  private class TimeLeft {
    private AtomicLong startTime;
    private AtomicLong endTime;

    TimeLeft (AtomicLong startTime, AtomicLong endTime) {
      this.startTime = startTime;
      this.endTime = endTime;
    }

    @Override
    public String toString() {
      final long start = startTime.get();
      final long end = endTime.get();

      if (start == end) {
        return "Experiment didn't start yet, still preprocessing.";
      }
      if (start > end) {
        return "Experiment is done, currently postprocessing.";
      }

      final Period p = new Period(start, end);

      return String.format("Approximately %s days, %s hours and %s minutes remain.",
          p.getDays(), p.getHours(), p.getMinutes());
    }
  }

  @Override
  public void startUp() {
    log.info("Initializing Executor.");

    // TODO(team): This will need to be fixed such that metrics can be made pipeline.specific.

    metricExporter.register(
        "remove_subject_from_experiment_count",
        "Counts the number of subjects removed from Experiments for exceptional reasons.",
        Metric.make(removeSubjectFromExperimentCount));

    metricExporter.register(
        "subject_missing_associated_process_count",
        "Counts the number of subjects missing associated UNIX processes.",
        Metric.make(subjectMissingAssociatedProcess));

    metricExporter.register(
        "subject_successfully_executed_count",
        "Counts the number of successfully executed subjects",
        Metric.make(successfullyExecutedSubjects));

    metricExporter.register(
        "restarted_subject_count",
        "Counts the number of times subjects have restarted across all experiments",
        Metric.make(restartedSubjectCount));

    metricExporter.register(
        "current_population_size",
        "The current population size of actively running subjects in the experiment",
        Metric.make(currentPopulationSize));
  }
}
