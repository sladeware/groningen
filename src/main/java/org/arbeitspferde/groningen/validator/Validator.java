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

package org.arbeitspferde.groningen.validator;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.eventlog.EventLoggerService;
import org.arbeitspferde.groningen.eventlog.SafeProtoLogger;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.PauseTime;
import org.arbeitspferde.groningen.experimentdb.ResourceMetric;
import org.arbeitspferde.groningen.experimentdb.SubjectRestart;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.proto.Event.EventEntry;
import org.arbeitspferde.groningen.proto.Event.EventEntry.Builder;
import org.arbeitspferde.groningen.proto.Event.EventEntry.FitnessScore;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Validator pipeline stage performs validation on subject group members to prevent grossly
 * degenerate individuals from polluting the population. For example, subjects that are flapping
 * excessively are invalidated to strongly signal to the {@link Hypothesizer} that it should not be
 * propagated into the next generation.
 */
@PipelineIterationScoped
public class Validator extends ProfilingRunnable {
  /** Logger for this class */
  private static final Logger logger = Logger.getLogger(Validator.class.getCanonicalName());

  /** The Experimental Database */
  private final ExperimentDb experimentDb;
  private final EventLoggerService eventLoggerService;
  private final String servingAddress;
  private final long startTime;
  private final MetricExporter metricExporter;

  private AtomicLong invalidDueToRestartThresholdCrossed = new AtomicLong(0);
  private AtomicLong invalidDueToNeverStarting = new AtomicLong(0);
  private AtomicLong invalidDueToCommandLineMismatch = new AtomicLong(0);
  private AtomicLong invalidDueToRemoval = new AtomicLong(0);

  @Inject
  public Validator(final Clock clock, final MonitorGroningen monitor, final ExperimentDb e,
      final EventLoggerService eventLoggerService,
      @Named("servingAddress") final String servingAddress,
      @Named("startTime") final Long startTime, final MetricExporter metricExporter) {
    super(clock, monitor);

    experimentDb = e;
    this.eventLoggerService = eventLoggerService;
    this.servingAddress = servingAddress;
    this.startTime = startTime;
    this.metricExporter = metricExporter;
  }

  @Override
  public void profiledRun(GroningenConfig config) {
    Experiment lastExperiment = null;
    lastExperiment = experimentDb.getExperiments().getLast();
    if (lastExperiment == null) {
      logger.warning("Experiments do not exist. Skipping Validator stage.");
    } else {
      for (final SubjectStateBridge subject : lastExperiment.getSubjects()) {
        final PauseTime pauseTime = subject.getPauseTime();
        final ResourceMetric resourceMetric = subject.getResourceMetric();
        final boolean subjectInvalid = invalidSubject(subject);

        if (subjectInvalid) {
          pauseTime.invalidate();
          resourceMetric.invalidate();
        }

        final GroningenParamsOrBuilder operatingParameters = config.getParamBlock();

        final double latencyWeight = operatingParameters.getLatencyWeight();
        final double throughputWeight = operatingParameters.getThroughputWeight();
        final double memoryWeight = operatingParameters.getMemoryWeight();

        final double latencyScore = pauseTime.computeScore(PauseTime.ScoreType.LATENCY);
        final double throughputScore = pauseTime.computeScore(PauseTime.ScoreType.THROUGHPUT);
        final double memoryScore = resourceMetric.computeScore(ResourceMetric.ScoreType.MEMORY);

        final String servingAddress = subject.getAssociatedSubject().getServingAddress();

        final long experimentId = experimentDb.getExperimentId();

        // TODO(team): This is a gross, over-broad categorization.

        final Event.EventEntry.Type result =
            subjectInvalid ? Event.EventEntry.Type.UNEXPECTED_DEATH :
            Event.EventEntry.Type.EXPERIMENT_END;

        final Builder eventBuilder = Event.EventEntry.newBuilder()
          .setSubjectServingAddress(servingAddress)
          .setGroningenServingAddress(servingAddress)
          .setExperimentId(experimentId)
          .setType(result)
          // We may want to get this from the state machine directly instead of deriving here.
          .setTime(clock.now().getMillis())
          .setGroningenStartTime(startTime);

        // TODO(team): Re-evaluate protocol buffer, and just fundamental data types to avoid casts.
        final FitnessScore latencyScoreType = FitnessScore.newBuilder()
            .setName("LATENCY")
            .setCoefficient((float) latencyWeight)
            .setScore((float) latencyScore)
            .build();
        eventBuilder.addScore(latencyScoreType);

        final FitnessScore throughputScoreType = FitnessScore.newBuilder()
            .setName("THROUGHPUT")
            .setCoefficient((float) throughputWeight)
            .setScore((float) throughputScore)
            .build();
        eventBuilder.addScore(throughputScoreType);

        final FitnessScore memoryScoreType = FitnessScore.newBuilder()
            .setName("MEMORY")
            .setCoefficient((float) memoryWeight)
            .setScore((float) memoryScore)
            .build();
        eventBuilder.addScore(memoryScoreType);

        // TODO(team): Handle command line arguments.

        eventBuilder.setGroningenConfiguration(config.getProtoConfig());

        for (final double pauseTimeDuration : subject.getPauseTime().getPauseDurations()) {
          eventBuilder.addPauseEventBuilder().setDurationInSeconds(pauseTimeDuration).build();
        }

        final SafeProtoLogger<EventEntry> safeProtoLogger = eventLoggerService.getLogger();

        Preconditions.checkNotNull(safeProtoLogger, "safeProtoLogger == null");

        try {
          safeProtoLogger.logProtoEntry(eventBuilder.build());
        } catch (final IOException e) {
          logger.log(Level.SEVERE, "Could not log event.", e);
        } catch (final IllegalArgumentException e) {
          logger.log(Level.SEVERE, "Error encoding the event emission.", e);
        }
      }
    }
  }

  /** Returns true iff the subject is invalid */
  private boolean invalidSubject(final SubjectStateBridge bridge) {
    boolean invalid = false;

    final SubjectRestart subjectRestart = bridge.getSubjectRestart();
    final StringBuilder subjectSignature = new StringBuilder();

    subjectSignature.append("[Subject Id: ");
    subjectSignature.append(bridge.getIdOfObject());
    subjectSignature.append("| Serving Address ");
    subjectSignature.append(bridge.getAssociatedSubject().getServingAddress());
    subjectSignature.append("]");

    // Excessively flapping subjects are invalid because they're really bad for user facing services
    // even if we are only talking about a single subject.
    if (subjectRestart.restartThresholdCrossed(this.experimentDb)) {
      invalid = true;
      invalidDueToRestartThresholdCrossed.incrementAndGet();
      logger.warning(
          String.format("%s invalidated for too many illegal restarts.", subjectSignature));
    }

    // A subject is invalid when it did not run in production, even though it is never actually
    // had a chance to be fairly scored. Alternatives are to propagate it into the next generation
    // without mutation or simply run it later after the initial set of experimental subjects run.
    // We believe invalidating it to be the lesser of these "evils".
    if (subjectRestart.didNotRun()) {
      invalid = true;
      invalidDueToNeverStarting.incrementAndGet();
      logger.warning(String.format("%s invalidated for having not run.", subjectSignature));
    }

    // Check if the subject's command line matches with the authoritative's command-line strings.
    final CommandLine commandLine = bridge.getCommandLine();
    final List<String> commandLineStrings = bridge.getCommandLineStrings();

    if (commandLine == null) {
      logger.warning(String.format("%s invalidated for lacking an associated CommandLine object.",
          subjectSignature));
    } else if (commandLineStrings.isEmpty()) {
      logger.warning(String.format("%s invalidated for lacking associated command line string.",
          subjectSignature));
    } else {
      final String cls = commandLine.toArgumentString().trim();
      for (final String commandLineString : commandLineStrings) {
        if ((cls != null) && (commandLineString.indexOf(cls) < 0)) {
          invalid = true;
          invalidDueToCommandLineMismatch.incrementAndGet();
          logger.warning(String.format(
              "%s invalidated due to command line mismatch: »%s« versus »%s«.",
              subjectSignature, commandLineString, cls));
        }
      }
    }

    // Subjects that were removed from an experiment are invalid
    if (bridge.wasRemoved()) {
      invalid = true;
      invalidDueToRemoval.incrementAndGet();
    }

    return invalid;
  }

  @Override
  public void startUp() {
    logger.info("Initializing Validator.");

    // TODO(team): This will need to be fixed such that metrics can be made pipeline.specific.

    metricExporter.register(
        "invalidate_due_to_restart",
        "Counts the number of invalidations due to crossing restart threshold.",
        Metric.make(invalidDueToRestartThresholdCrossed));
    metricExporter.register(
        "invalidate_due_to_did_not_run",
        "Counts the number of invalidations due to a subject failing to not starting.",
        Metric.make(invalidDueToNeverStarting));
    metricExporter.register(
        "invalidate_due_to_command_line_mismatch",
        "Counts the number of invalidations due to command-line string mismatch.",
        Metric.make(invalidDueToCommandLineMismatch));
    metricExporter.register(
        "invalidate_due_to_removal",
        "Counts the number of invalidations due to removal from the experiment.",
        Metric.make(invalidDueToRemoval));
  }
}
