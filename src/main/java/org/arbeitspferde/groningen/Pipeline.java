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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.display.Displayable;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.generator.SubjectShuffler;
import org.arbeitspferde.groningen.hypothesizer.Hypothesizer;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class encapsulates a Groningen pipeline.
 *
 * TODO(team): Current implementation doesn't allow more than one Pipeline instance
 * to be used simultaneously (see pipelineIterationCount metrics, for example). Explicitly
 * marked this class with {@link Singleton} annotation for a while.
 */
@PipelineScoped
public class Pipeline {
  private static final Logger log = Logger.getLogger(Pipeline.class.getCanonicalName());

  @Inject
  @NamedConfigParam("subjects_to_display")
  private Provider<Integer> subjectsToDisplay = new Provider<Integer>() {
    @Override
    public Integer get() {
      return GroningenParams.getDefaultInstance().getSubjectsToDisplay();
    }
  };

  private final BlockScope pipelineIterationScope;

  private final Displayable displayable;

  private final MonitorGroningen monitor;

  private final Provider<SubjectShuffler> shuffler;

  private final Hypothesizer hypothesizer;

  private final ConfigManager configManager;

  private final PipelineId pipelineId;

  private final Provider<PipelineIteration> pipelineIterationProvider;

  private final PipelineStageDisplayer pipelineStageDisplayer;
  
  private final Thread pipelineThread;

  /** Counts the number of pipeline iterations */
  private AtomicLong pipelineIterationCount = new AtomicLong(1);
  
  private PipelineIteration currentIteration;

  private AtomicBoolean isKilled;

  private static class PipelineStageDisplayer {
    private PipelineIteration currentIteration;
    private final String[] stages = {"Hypothesizer", "Generator", "Executor", "Validator"};

    public synchronized void setCurrentIteration(PipelineIteration currentIteration) {
      this.currentIteration = currentIteration;
    }

    @Override
    public synchronized String toString() {
      return stages[currentIteration != null ? currentIteration.getStage() : 0];
    }
  }
  
  private synchronized void setCurrentIteration(PipelineIteration currentIteration) {
    this.currentIteration = currentIteration;
  }

  @Inject
  public Pipeline(
    @Named(PipelineIterationScoped.SCOPE_NAME) BlockScope pipelineIterationScope,
    Displayable displayable,
    MonitorGroningen monitor,
    Provider<SubjectShuffler> shuffler,
    Hypothesizer hypothesizer,
    ConfigManager configManager,
    Provider<PipelineIteration> pipelineIterationProvider,
    PipelineId pipelineId,
    final MetricExporter metricExporter) {

    this.pipelineIterationScope = pipelineIterationScope;
    this.displayable = displayable;
    this.monitor = monitor;
    this.shuffler = shuffler;
    this.hypothesizer = hypothesizer;
    this.configManager = configManager;
    this.pipelineIterationProvider = pipelineIterationProvider;
    this.pipelineId = pipelineId;
    this.pipelineThread = Thread.currentThread();

    isKilled = new AtomicBoolean();
    pipelineStageDisplayer = new PipelineStageDisplayer();

    metricExporter.register(
        "pipeline_iteration_count",
        "Counts the number of complete Groningen processing pipeline iterations",
         Metric.make(pipelineIterationCount));
  }

  public PipelineId id() {
    return pipelineId;
  }

  public synchronized PipelineIteration currentIteration() {
    return currentIteration;
  }

  public void kill() {
    isKilled.set(true);
  }

  public void joinPipeline() throws InterruptedException {
    pipelineThread.join();
  }
  
  /**
   * Ready the monitoring.
   */
  private void configureMonitoring() {
    /** Export monitored variables */
    monitor.monitorObject(pipelineIterationCount,
        "Pipeline iteration count");
    monitor.monitorObject(pipelineStageDisplayer,
           "Current pipeline stage");
  }

  public void run() {
    try {
      GroningenConfig firstConfig = configManager.queryConfig();
      configureMonitoring();

      boolean firstIteration = true;
      boolean notCompleted = true;
      do {
        pipelineIterationScope.enter();

        try {
          GroningenConfig config = configManager.queryConfig();
          GroningenConfigParamsModule.nailConfigToScope(config, pipelineIterationScope);
          /*
           * TODO(team): Seeding these objects here is not good. In an ideal world they
           * should be created per-pipeline
           */
          PipelineIteration iteration = pipelineIterationProvider.get();
          setCurrentIteration(currentIteration);
          
          pipelineStageDisplayer.setCurrentIteration(iteration);
          monitor.maxIndividuals(subjectsToDisplay.get());

          if (firstIteration) {
            /*
             * the population size within the ga engine is currently fixed once the engine is
             * instantiated. Hence, there is no benefit to recalculating each time through the
             * loop.
             */
            try {
              hypothesizer.setPopulationSize(shuffler.get().createIterator().getSubjectCount());
            } catch (RuntimeException e) {
              log.log(Level.SEVERE,
                  "unable to gather number of tasks in specified jobs. Hence unable to " +
                      "initialize hypothesizer.  Aborting...", e);
              throw e;
            }
            firstIteration = false;
          }

          notCompleted = iteration.run();
        } finally {
          pipelineIterationScope.exit();
        }
        pipelineIterationCount.incrementAndGet();
      } while (notCompleted  && !isKilled.get());
    } catch (Exception e) {
      log.log(Level.SEVERE, "Fatal error", e);
      throw new RuntimeException(e);
    }
  }

  public Displayable getDisplayable() {
    return displayable;
  }
}
