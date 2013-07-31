/* Copyright 2013 Google, Inc.
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
package org.arbeitspferde.groningen.eventlog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.PauseTime;
import org.arbeitspferde.groningen.experimentdb.ResourceMetric;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.proto.Event.EventEntry;
import org.arbeitspferde.groningen.proto.Event.EventEntry.Builder;
import org.arbeitspferde.groningen.proto.Event.EventEntry.FitnessScore;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A logger that records stats on the performance of a given subject.
 * 
 * Protocol buffer based logs are written to the supplied {@link EventLoggerService} for deeper
 * analysis and trending.
 */
public class SubjectEventProtoLogger implements SubjectEventLogger {
  /** Logger for this class */
  private static final Logger logger =
      Logger.getLogger(SubjectEventProtoLogger.class.getCanonicalName());

  private final Clock clock;

  /** The event logging service to which to submit the composed events. */
  private final EventLoggerService eventLoggerService;

  /** The groningen server address (added to log events). */
  private final String groningenServingAddress;
  
  /** The beginning time of this experiment iteration. */
  private final long startTime;
  
  private final MetricExporter metricExporter;
  
  @Inject
  public SubjectEventProtoLogger(Clock clock, 
      final EventLoggerService eventLoggerService,
      @Named("servingAddress") final String groningenServingAddress,
      @Named("startTime") final Long startTime, MetricExporter metricExporter) {
    this.clock = clock;
    this.eventLoggerService = eventLoggerService;
    this.groningenServingAddress = groningenServingAddress;
    this.startTime = startTime;
    this.metricExporter = metricExporter;
  }

  /**
   * 
   * @see SubjectEventLogger#logSubjectInExperiment(GroningenConfig, Experiment, 
   *    SubjectStateBridge)
   */
  @Override
  public void logSubjectInExperiment(
      GroningenConfig config, Experiment experiment, SubjectStateBridge subject) {
    final SafeProtoLogger<EventEntry> safeProtoLogger = eventLoggerService.getLogger();
    Preconditions.checkNotNull(safeProtoLogger, "safeProtoLogger == null");
    
    long experimentId = experiment.getIdOfObject();
    final Event.EventEntry event = composeEvent(config, experimentId, subject);

    try {
      safeProtoLogger.logProtoEntry(event);
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "Could not log event.", e);
    } catch (final IllegalArgumentException e) {
      logger.log(Level.SEVERE, "Error encoding the event emission.", e);
    }    
  }

  /**
   * Compose an Event to be logged from a given @{SubjectStateBridge}.
   *  
   *  @param config the active groningen config for this experiment iteration.
   *  @param experimentId the experiment iteration number.
   *  @param subject the subject for which to create the @{EventEntry}.
   *  @return a protocol buffer log entry summarizing the subject's performance.
   */
  @VisibleForTesting
  EventEntry composeEvent(GroningenConfig config, long experimentId, SubjectStateBridge subject) {
    final PauseTime pauseTime = subject.getPauseTime();
    final ResourceMetric resourceMetric = subject.getResourceMetric();

    final GroningenParamsOrBuilder operatingParameters = config.getParamBlock();

    final double latencyWeight = operatingParameters.getLatencyWeight();
    final double throughputWeight = operatingParameters.getThroughputWeight();
    final double memoryWeight = operatingParameters.getMemoryWeight();

    final double latencyScore = pauseTime.computeScore(PauseTime.ScoreType.LATENCY);
    final double throughputScore = pauseTime.computeScore(PauseTime.ScoreType.THROUGHPUT);
    final double memoryScore = resourceMetric.computeScore(ResourceMetric.ScoreType.MEMORY);

    final String processServingAddress = subject.getAssociatedSubject().getServingAddress();

    // TODO(team): This is a gross, over-broad categorization.
    
    final Event.EventEntry.Type result =
        subject.isInvalid() == Boolean.FALSE ? Event.EventEntry.Type.EXPERIMENT_END :
            Event.EventEntry.Type.UNEXPECTED_DEATH;

     final Builder eventBuilder = Event.EventEntry.newBuilder()
       .setSubjectServingAddress(processServingAddress)
       .setGroningenServingAddress(groningenServingAddress)
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

    return eventBuilder.build();
  }
}
