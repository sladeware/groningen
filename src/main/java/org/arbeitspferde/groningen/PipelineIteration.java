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

package org.arbeitspferde.groningen;

import com.google.inject.Inject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.executor.Executor;
import org.arbeitspferde.groningen.generator.Generator;
import org.arbeitspferde.groningen.hypothesizer.Hypothesizer;
import org.arbeitspferde.groningen.scorer.IterationScorer;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.validator.Validator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * This class encapsulates the logic of one pipeline iteration. It's assumed that this class
 * is used withing @PipelineIterationScoped Guice scope with all the dependencies injected or
 * seeded by Guice.
 *
 * TODO(team): Current implementation doesn't allow more than one PipelineIteration instance
 * to be used simultaneously (see currentPipelineStage metrics, for example).
 */
@PipelineIterationScoped
public class PipelineIteration {
  private static final Logger log = Logger.getLogger(PipelineIteration.class.getCanonicalName());

  private final GroningenConfig config;
  private final PipelineSynchronizer pipelineSynchronizer;
  private final Executor executor;
  private final Generator generator;
  private final Hypothesizer hypothesizer;
  private final Validator validator;
  private final IterationScorer scorer;
  private final PipelineStageInfo pipelineStageInfo;

  /**
   * Track the pipestage we are in. It's fine to have this metrics defined this way as there's only
   * one pipeline per Groningen instance.
   *
   * TODO(team): fix when we have multiple pipelines
   **/
  private static final AtomicInteger currentPipelineStage = new AtomicInteger();

  @Inject
  public PipelineIteration(final GroningenConfig config,
      final PipelineSynchronizer pipelineSynchronizer,
      final Executor executor,
      final Generator generator,
      final Hypothesizer hypothesizer,
      final Validator validator,
      final IterationScorer scorer,
      final MetricExporter metricExporter,
      final PipelineStageInfo pipelineStageInfo) {

    this.config = config;
    this.pipelineSynchronizer = pipelineSynchronizer;
    this.executor = executor;
    this.generator = generator;
    this.hypothesizer = hypothesizer;
    this.validator = validator;
    this.scorer = scorer;
    this.pipelineStageInfo = pipelineStageInfo;

    metricExporter.register(
        "current_pipeline_stage",
        "Current stage: 0=Hypothesizer, 1=Generator, 2=Executor, 3=Validator 4=Scorer",
        Metric.make(currentPipelineStage));
  }

  public int getStage() {
    return currentPipelineStage.get();
  }

  /**
   * Provide the remaining time within the run of the experiment.
   *
   * This does not include any restart or wait times and is only valid when the experiment
   * is running.
   *
   * @return time in secs remaining in the experiment, -1 if the request was made outside the
   *    valid window.
   */
  public int getRemainingExperimentalSecs() {
    return (int) executor.getRemainingDurationSeconds();
  }

  public boolean run() {
    executor.startUp();
    generator.startUp();
    hypothesizer.startUp();
    validator.startUp();
    scorer.startUp();

    currentPipelineStage.set(0);

    // Synchronization within the pipeline iteration - after the config is updated
    pipelineSynchronizer.iterationStartHook();

    pipelineStageInfo.set(PipelineStageState.HYPOTHESIZER);
    hypothesizer.run(config);
    boolean notComplete = hypothesizer.notComplete();

    if (notComplete) {
      currentPipelineStage.set(1);
      pipelineStageInfo.set(PipelineStageState.GENERATOR);
      generator.run(config);

      // This stage returns only when all experiments are complete
      currentPipelineStage.set(2);
      pipelineSynchronizer.executorStartHook();
      executor.run(config);

      currentPipelineStage.set(3);
      pipelineStageInfo.set(PipelineStageState.SCORING);
      validator.run(config);

      currentPipelineStage.set(4);
      scorer.run(config);
    }

    return notComplete;
  }
}
