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
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.generator.Generator;
import org.arbeitspferde.groningen.hypothesizer.Hypothesizer;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.validator.Validator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
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

  /** The delay period in millis betwen retries of failing Generator runs */
  private static final long RUN_GENERATOR_RETRY_DELAY = 60000L;

  private final GroningenConfig config;
  private final PipelineSynchronizer pipelineSynchronizer;
  private final ExperimentDb experimentDb;
  private final Executor executor;
  private final Generator generator;
  private final Hypothesizer hypothesizer;
  private final Validator validator;

  /**
   * Track the pipestage we are in. It's fine to have this metrics defined this way as there's only
   * one pipeline per Groningen instance.
   *
   * TODO(team): fix when we have multiple pipelines
   **/
  private static AtomicInteger currentPipelineStage = new AtomicInteger();

  @Inject
  public PipelineIteration(final GroningenConfig config,
      final PipelineSynchronizer pipelineSynchronizer,
      final ExperimentDb experimentDb,
      final Executor executor,
      final Generator generator,
      final Hypothesizer hypothesizer,
      final Validator validator,
      final MetricExporter metricExporter) {

    this.config = config;
    this.pipelineSynchronizer = pipelineSynchronizer;
    this.experimentDb = experimentDb;
    this.executor = executor;
    this.generator = generator;
    this.hypothesizer = hypothesizer;
    this.validator = validator;

    metricExporter.register(
        "current_pipeline_stage",
        "Current stage: 0=Hypothesizer, 1=Generator, 2=Executor, 3=Validator",
        Metric.make(currentPipelineStage));
  }

  public int getStage() {
    return currentPipelineStage.get();
  }

  public boolean run() {
    executor.startUp();
    generator.startUp();
    hypothesizer.startUp();
    validator.startUp();

    currentPipelineStage.set(0);

    // Synchronization within the pipeline iteration - after the config is updated
    pipelineSynchronizer.iterationStartHook();

    experimentDb.putArguments(config.getParamBlock().getDuration(),
        config.getParamBlock().getRestart());

    hypothesizer.run(config);
    boolean notComplete = hypothesizer.notComplete();

    if (notComplete) {
      // Run the Generator and retry until it succeeds
      currentPipelineStage.set(1);
      boolean done;
      do {
        done = true;
        try {
          generator.run(config);
        } catch (final RuntimeException re) {
          done = false;
          log.log(Level.WARNING, "Problems running the Generator. Retrying in 1 minute.", re);
          try {
            Thread.sleep(RUN_GENERATOR_RETRY_DELAY);
          } catch (final InterruptedException ie) {
            log.log(Level.WARNING, "Problems sleeping while retrying the Generator.", ie);
          }
        }
      } while (!done);

      // This stage returns only when all experiments are complete
      currentPipelineStage.set(2);
      pipelineSynchronizer.executorStartHook();
      executor.run(config);

      currentPipelineStage.set(3);
      validator.run(config);
    }

    pipelineSynchronizer.finalizeCompleteHook();

    return notComplete;
  }
}
