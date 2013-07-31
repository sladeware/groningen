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

package org.arbeitspferde.groningen.experimentdb;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.proto.ExperimentDbProtos;
import org.arbeitspferde.groningen.subject.Subject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.arbeitspferde.groningen.experimentdb.ExperimentDb.write;

/**
 * Subject class designed to be the main gateway to maintaining state on a
 * particular subject.
 *
 * This class is not thread safe.
 */
public class SubjectStateBridge extends InMemoryCache.Value<SubjectStateBridge> {
  private static final Logger log = Logger.getLogger(SubjectStateBridge.class.getCanonicalName());

  @VisibleForTesting
  static final int GC_EVENT_JOB_SIZE = 1000;

  /**
   * The underlying subject that this bridge to the cluster environment retrieves state for and
   * refers to.
   */
  private Subject subject;

  /** The state of this subject */
  private State state = State.NEW;

  /** The epoch time in seconds that we detected this subject is unhealthy */
  private long timestamp = 0;

  /**
   * Possible parents for some of the parsed information (used by the parser to
   * pass context information)
   */
  public enum Parent {
    TOP, CMS, PARNEW, CONCURRENT_MODE_FAILURE
  }

  /**
   * Possible subject states:
   *
   * NEW = Initial state. A newly created subject that has never been probed by the Executor.
   * HEALTHY = Running state. A good subject that successfully passes health verification probes.
   * UNHEALTHY = Running state. A bad subject that is failing health verification probes.
   * DEAD = Terminal state. A bad subject that we have removed from the experiment.
   */
  public enum State {
    NEW, HEALTHY, UNHEALTHY, DEAD
  }

  /** Command line of this subject's subject */
  @Nullable private CommandLine commandLine = null;

  /** Whether subject was invalidated - use null to mark that this has not been set */
  @Nullable private Boolean invalidated = null;
  
  /** Evaluated version of the subject - null means the subject has not been evaluated */
  @Nullable private EvaluatedSubject evaluatedCopy = null;
  
  /** Pause time metrics for this subject */
  private final PauseTime pauseTime = new PauseTime();

  /** Resource usage metrics for this subject */
  private final ResourceMetric resourceMetric = new ResourceMetric();

  /** Restart information for this subject */
  private final SubjectRestart subjectRestart = new SubjectRestart();

  /** Information collected so far on current GC event */
  private Optional<ExperimentDbProtos.Gc> gcInfo = Optional.absent();


  /** Sequence number for GC events */
  private int gcInfoCounter;

  /** True iff the subject was removed by the Executor from an experiment */
  private boolean removed = false;

  /** Command-line strings for this subject */
  private List<String> commandLineStrings = Lists.newArrayListWithExpectedSize(10);

  /**
   * Creates an instance of this class with the given subject id
   *
   * @param experimentDb Experiment database
   * @param id Subject id
   */
  SubjectStateBridge(ExperimentDb experimentDb, long id) {
    super(id);
    checkNotNull(experimentDb);
  }

  /** Adds the given command-line string used in this subject */
  public void addCommandLineString(String commandLineString) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(commandLineString),
        "Invalid command-line string %s", commandLineString);
    commandLineStrings.add(commandLineString);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public State getState() {
    return state;
  }

  public void setState(final State state) {
    log.info(String.format("Subject %s moving state from %s to %s.", getHumanIdentifier(),
        this.state, state));

    this.state = state;
  }

  /** Mark the subject as invalid within the experiment. */
  public void markInvalid() {
    invalidated = Boolean.TRUE;
  }
  
  /** Mark the subject as valid within the experiment. */
  public void markValid() {
    invalidated = Boolean.FALSE;    
  }
  
  /**
   * Whether the subject has been removed from the experiment.
   * 
   * @return TRUE if the subject has been marked invalid, FALSE if it has been marked valid, null
   *    if no determination of the subjects validity has yet been made.
   */
  @Nullable
  public Boolean isInvalid() {
    return invalidated;
  }

  /** Returns command-line strings used in this subject */
  public List<String> getCommandLineStrings() {
    return Lists.newArrayList(commandLineStrings);
  }

  /** Return command line of this subject */
  public CommandLine getCommandLine() {
    return commandLine;
  }
  
  /** Store the evaluated version of this subject */
  public void setEvaluatedCopy(EvaluatedSubject evaluatedCopy) {
    this.evaluatedCopy = evaluatedCopy;
  }
  
  /** 
   * Getter for the stored {@link EvaluatedSubject} associated with this subject.
   * 
   * @returns the evaluated subject iff the subject has been scored and null if not.
   */
  public EvaluatedSubject getEvaluatedCopy() {
    return evaluatedCopy;
  }

  /** Return pause time metrics for this subject */
  public PauseTime getPauseTime() {
    return pauseTime;
  }

  /** Return resource usage metrics for this subject */
  public ResourceMetric getResourceMetric() {
    return resourceMetric;
  }

  /** Return restart information for this subject */
  public SubjectRestart getSubjectRestart() {
    return subjectRestart;
  }

  /** Returns the number of millis a subject can be unhealthy */
  public long getWarmupTimeoutMillis() {
    if (subject != null) {
      return 1000 * subject.getWarmupTimeout();
    } else {
      return 0;
    }
  }

  /** Returns true iff the subject was removed from an experiment */
  public boolean wasRemoved() {
    return removed;
  }

  /** Mark that this subject has been removed from an experiment */
  public void removeFromExperiment() {
    removed = true;
  }

  /**
   * There is a one-to-one correspondence between the bridge in our and the cluster groups for the
   * experiment. This method returns the subject associated with this bridge.
   *
   * @return the {@link Subject} associated with this bridge.
   */
  public Subject getAssociatedSubject() {
    return subject;
  }

  public void setAssociatedSubject(final Subject subject) {
    this.subject = subject;
  }

  /**
   * Store a new record representing a subject's command line we're optimizing.
   */
  public void storeCommandLine(final JvmFlagSet jvmFlagSet) {

    commandLine = new CommandLine(jvmFlagSet);


    resourceMetric.setMemoryFootprint(commandLine.getHeapSize());

    write("storeCommandLine",
        getIdOfObject(),
        commandLine.getHeapSize(),
        commandLine.getAdaptiveSizeDecrementScaleFactor(),
        commandLine.getCmsExpAvgFactor(),
        commandLine.getCmsIncrementalDutyCycle(),
        commandLine.getCmsIncrementalDutyCycleMin(),
        commandLine.getCmsIncrementalOffset(),
        commandLine.getCmsIncrementalSafetyFactor(),
        commandLine.getCmsInitiatingOccupancyFraction(),
        commandLine.getGcTimeRatio(),
        commandLine.getMaxGcPauseMillis(),
        commandLine.getMaxHeapFreeRatio(),
        commandLine.getMinHeapFreeRatio(),
        commandLine.getNewRatio(),
        commandLine.getNewSize(),
        commandLine.getParallelGCThreads(),
        commandLine.getSurvivorRatio(),
        commandLine.getTenuredGenerationSizeIncrement(),
        commandLine.getYoungGenerationSizeIncrement(),
        commandLine.getSoftRefLruPolicyMsPerMb(),
        commandLine.getCmsIncrementalMode(),
        commandLine.getCmsIncrementalPacing(),
        commandLine.getUseCmsInitiatingOccupancyOnly(),
        commandLine.getUseConcMarkSweepGC(),
        commandLine.getUseParallelGC(),
        commandLine.getUseParallelOldGC(),
        commandLine.getUseSerialGC());
  }

  /*
   * TODO(team): The next 6 methods will need to be reworked when the new
   * parsing system is incorporated. They are retained less as functionally
   * active used, and more as a template for megastore interaction, and to
   * allow/retain existing megastore tests, both of which will be needed again
   * after the rework.
   *
   * drk @ 6/14/2012: Megastore dependency was removed.
   */
  /**
   * Start processing a new GC log entry
   */
  public void startTop() {
    ExperimentDbProtos.Gc.Builder gcBuilder = ExperimentDbProtos.Gc.newBuilder();
    gcBuilder.setSubjectId(getIdOfObject());
    gcBuilder.setId(++gcInfoCounter);

    gcInfo = Optional.of(gcBuilder.build());
  }


  /** Store a new record in the Top GC Log table, which is the root table. */
  public void storeTop(boolean fullGc, int gcDeltaSize, int gcEndSize, int gcStartSize,
                       boolean partialGc, int recordCounter, double relativeTimestamp) {

    final ExperimentDbProtos.Gc.Builder gcInfoMutator =
        gcInfo.or(ExperimentDbProtos.Gc.getDefaultInstance()).toBuilder();

    gcInfoMutator.setFull(fullGc);
    gcInfoMutator.setDeltaSize(gcDeltaSize);
    gcInfoMutator.setEndSize(gcEndSize);
    gcInfoMutator.setStartSize(gcStartSize);
    gcInfoMutator.setPartial(partialGc);
    gcInfoMutator.setRecordCounter(recordCounter);
    gcInfoMutator.setRelativeTimestamp(relativeTimestamp);

    gcInfo = Optional.of(gcInfoMutator.build());

    write("storeTopSubject", getIdOfObject(), fullGc, gcDeltaSize, gcEndSize, gcStartSize,
        partialGc, recordCounter, relativeTimestamp);

    summarizeGcEvent();
  }

  /**
   * This method is run once each GC event is completely parsed. It collects
   * summary statistics (to be logged with the Subject), and summary information
   * for later use by the Hypothesizer.
   */
  private void summarizeGcEvent() {
    // TODO(team): Reenable this when we can trust the parser and then remove code from LogFilter
    // if (gcInfo.hasConcurrentModeFailure()) {
    //   pauseTime.incrementPauseTime(gcInfo.getConcurrentModeFailure().getPermTime());
    // }
    // if (gcInfo.hasParNew()) {
    //   pauseTime.incrementPauseTime(gcInfo.getParNew().getParNewTime());
    // }
    // if (gcInfo.hasCmsInitialMark()) {
    //   pauseTime.incrementPauseTime(gcInfo.getICms().getTime());
    // }
    // if (gcInfo.hasCmsRemark()) {
    //   pauseTime.incrementPauseTime(gcInfo.getICms().getTime());
    // }
    // no summary stats yet
  }

  /*
   * ***************************************************
   * TODO(team): the remaining methods are all used by the old fragile parsing system and are
   * retained because removing them introduces more risk of breaking the parser. When the parsing
   * system is replaced these should be deleted or reworked.
   * ***************************************************
   */
  /*
   * Store a new record in the Incremental Concurrent Mark and Sweep GC Log table.
   */
  public void storeICms(Parent parent, double time, int icmsDc) {
  }

  /** Store a new record in the Concurrent Mark and Sweep GC Log table. */
  public void storeCms(double time) {
  }

  /** Store a new GC record representing the CmsRemark phase. */
  public void storeCmsRemark(int startSize, int totalSize) {
  }

  /** Store a new record representing the CmsAbortPrecleanDueToTime phase. */
  public void storeCmsAbortPrecleanDueToTime(Parent parent, double time) {
  }

  /** Store a new record representing the WeakRefsProcessing phase. */
  public void storeWeakRefsProcessing(double time, double timestamp) {
  }

  /** Store a new record representing a ClassUnloading GC Log event. */
  public void storeClassUnloading(double time, double timestamp, List<String> className) {
  }

  /** Store a new record representing the ScrubSymbolAndStringTables phase. */
  public void storeScrubSymbolAndStringTables(double time) {
  }

  /** Store a new record representing the RescanParallel phase. */
  public void storeRescanParallel(double time, double timestamp) {
  }

  /** Store a new record representing the YgOccupancy phase. */
  public void storeYgOccupancy(int youngGenCurrentOccupancy, int youngGenTotalSize,
    double timestamp) {
  }

  /** Store a new record representing the ConcurrentModeFailure phase. */
  public void storeConcurrentModeFailure(int cmsGenSize, int cmsEndSize, double cmsTime,
      int permStartSize, int permEndSize, int permSize, double permTime, int tenuredStartSize,
      int tenuredEndSize, int tenuredSize, int cmsStartSize) {
  }

  /** Store a new record representing the SystemTimes phase. */
  public void storeSystemTimes(Parent parent, double real, double sys, double user) {
  }

  /** Store a new record representing the CmsConcurrentPreclean phase. */
  public void storeCmsConcurrentPreclean(double cpuTime, double wallTime) {
  }

  /** Store a new record representing the CmsConcurrentMark phase. */
  public void storeCmsConcurrentMark(double cpuTime, double wallTime) {
  }

  /** Store a new record representing the CmsConcurrentReset phase. */
  public void storeCmsConcurrentReset(double cpuTime, double wallTime) {
  }

  /** Store a new record representing the CmsConcurrentSweep phase. */
  public void storeCmsConcurrentSweep(double cpuTime, double wallTime) {
  }

  /** Store a new record representing the CmsConcurrentMarkStart phase. */
  public void storeCmsConcurrentMarkStart() {
  }

  /** Store a new record representing the CmsConcurrentSweepStart phase. */
  public void storeCmsConcurrentSweepStart() {
  }

  /** Store a new record representing the CmsConcurrentResetStart phase. */
  public void storeCmsConcurrentResetStart() {
  }

  /** Store a new record representing the CmsConcurrentPrecleanStart phase. */
  public void storeCmsConcurrentPrecleanStart() {
  }

  /**
   * Store a new record representing the CmsConcurrentAbortablePrecleanStart phase.
   */
  public void storeCmsConcurrentAbortablePrecleanStart() {
  }

  /** Store a new record representing the CmsConcurrentAbortablePreclean phase. */
  public void storeCmsConcurrentAbortablePreclean(Parent parent, double cpuTime, double wallTime) {
  }

  /** Store a new record representing the CmsInitialMark phase. */
  public void storeCmsInitialMark(int tenuredMaxSize, int triggeredAtOccupancySize) {
  }

  /** Store a new record representing the ParNew (Young Generation) phase. */
  public void storeParNew(double abortTime, List<Integer> ages, int desiredSurvivorSize,
      int maxThreshold, int newThreshold, double parNewTime, boolean promotionFailed,
      List<Integer> ageSurvivorSizes, int survivorStartSize, int survivorEndSize,
      int survivorDeltaSize, List<Integer> ageTotalSurvivorSizes) {
  }

  public String getHumanIdentifier() {
    return String.format("[ID: %s | Subject: %s]", getIdOfObject(), getAssociatedSubject());
  }
}
