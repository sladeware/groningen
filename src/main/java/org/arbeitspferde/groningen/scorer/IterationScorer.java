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
package org.arbeitspferde.groningen.scorer;

import com.google.inject.Inject;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.eventlog.SubjectEventLogger;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.util.logging.Logger;

/**
 * A pipeline stage to score and record the scores for a given iteration.
 */
@PipelineIterationScoped
public class IterationScorer extends ProfilingRunnable {
  /** Logger for this class */
  private static final Logger logger =
      Logger.getLogger(IterationScorer.class.getCanonicalName());

  private final ExperimentDb experimentDb;
  private final SubjectScorer scorer;
  private final SubjectEventLogger subjectEventLogger;
  private final MetricExporter metricExporter;

  @Inject
  public IterationScorer(final Clock clock, final MonitorGroningen monitor, final ExperimentDb e,
      final SubjectScorer scorer, SubjectEventLogger subjectEventLogger,
      final MetricExporter metricExporter) {
    super(clock, monitor);

    experimentDb = e;
    this.scorer = scorer;
    this.metricExporter = metricExporter;
    this.subjectEventLogger = subjectEventLogger;
  }

  /** @see org.arbeitspferde.groningen.profiling.ProfilingRunnable#startUp() */
  @Override
  public void startUp() {
  }

  /**
   * Score the iteration and save off the values
   *
   * @see ProfilingRunnable#profiledRun(GroningenConfig)
   */
  @Override
  public void profiledRun(GroningenConfig config) {
    Experiment thisExperimentIteration = experimentDb.getLastExperiment();
    if (thisExperimentIteration == null) {
      logger.warning("Experiments do not exist. Skipping Scoring stage.");
      return;
    }

    final long experimentId = experimentDb.getExperimentId();

    for (final SubjectStateBridge subject : thisExperimentIteration.getSubjects()) {
      double score = scorer.compute(subject, config);

      // TODO(team): instantiate via a provider or assistedfactory to ease testing dependencies
      final EvaluatedSubject evaledSubject =
          new EvaluatedSubject(clock, subject, score, experimentId);
      subject.setEvaluatedCopy(evaledSubject);
      monitor.addIndividual(evaledSubject);

      subjectEventLogger.logSubjectInExperiment(
          config, thisExperimentIteration, subject);
    }

    /* With all subjects evaluated, we can close out this generation in the monitor */
    monitor.processGeneration();
  }
}
