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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.HistoryDatastore.HistoryDatastoreException;
import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.Displayable;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.FitnessScore;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.generator.SubjectShuffler;
import org.arbeitspferde.groningen.hypothesizer.Hypothesizer;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.MetricExporter;

import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.List;
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
  private final Provider<Integer> subjectsToDisplay = new Provider<Integer>() {
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

  private final ExperimentDb experimentDb;

  private final Datastore datastore;

  private final HistoryDatastore historyDatastore;

  private final Clock clock;

  private final ConfigManager configManager;

  private final PipelineId pipelineId;

  private final Provider<PipelineIteration> pipelineIterationProvider;

  private final PipelineStageDisplayer pipelineStageDisplayer;

  private final Thread pipelineThread;

  private final PipelineSynchronizer pipelineSynchronizer;

  /** Counts the number of pipeline iterations */
  private final AtomicLong pipelineIterationCount = new AtomicLong(1);

  private PipelineIteration currentIteration;

  private final AtomicBoolean isKilled;

  private GroningenConfig currentIterationConfig = null;

  private final PipelineStageInfo pipelineStageInfo;

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
    Datastore datastore,
    HistoryDatastore historyDatastore,
    ExperimentDb experimentDb,
    Clock clock,
    ConfigManager configManager,
    Provider<PipelineIteration> pipelineIterationProvider,
    PipelineId pipelineId,
    final MetricExporter metricExporter,
    PipelineSynchronizer pipelineSynchronizer,
    PipelineStageInfo pipelineStageInfo) {

    this.pipelineIterationScope = pipelineIterationScope;
    this.displayable = displayable;
    this.monitor = monitor;
    this.shuffler = shuffler;
    this.hypothesizer = hypothesizer;
    this.datastore = datastore;
    this.historyDatastore = historyDatastore;
    this.clock = clock;
    this.configManager = configManager;
    this.pipelineIterationProvider = pipelineIterationProvider;
    this.pipelineId = pipelineId;
    this.pipelineSynchronizer = pipelineSynchronizer;
    this.pipelineThread = Thread.currentThread();
    this.experimentDb = experimentDb;
    this.pipelineStageInfo = pipelineStageInfo;

    isKilled = new AtomicBoolean();
    pipelineStageDisplayer = new PipelineStageDisplayer();
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

  public void restoreState(PipelineState state) {
    if (!pipelineId.equals(state.pipelineId())) {
      throw new RuntimeException("trying to assign state from another pipeline");
    }
    experimentDb.reset(state.experimentDb());
  }

  public PipelineState state() {
    return new PipelineState(pipelineId, configManager.queryConfig(), experimentDb);
  }

  public PipelineHistoryState historyState() {
    List<EvaluatedSubject> evaluatedSubjects =
        new ArrayList<>();
    for (SubjectStateBridge ssb : experimentDb.getLastExperiment().getSubjects()) {
      evaluatedSubjects.add(new EvaluatedSubject(clock, ssb, FitnessScore.compute(ssb,
          configManager.queryConfig()), experimentDb.getExperimentId()));
    }
    return new PipelineHistoryState(pipelineId,
        configManager.queryConfig(),
        Instant.now(),
        evaluatedSubjects.toArray(new EvaluatedSubject[] {}),
        experimentDb.getExperimentId());
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

  public GroningenConfig getConfig() {
    return currentIterationConfig;
  }

  public void run() {
    try {
      GroningenConfig firstConfig = configManager.queryConfig();
      currentIterationConfig = firstConfig;
      configureMonitoring();

      boolean firstIteration = true;
      boolean notCompleted = true;
      do {
        pipelineIterationScope.enter();

        try {
          GroningenConfig config = configManager.queryConfig();
          currentIterationConfig = config;
          GroningenConfigParamsModule.nailConfigToScope(config, pipelineIterationScope);
          /*
           * TODO(team): Seeding these objects here is not good. In an ideal world they
           * should be created per-pipeline
           */
          PipelineIteration iteration = pipelineIterationProvider.get();
          setCurrentIteration(iteration);

          pipelineStageDisplayer.setCurrentIteration(iteration);
          monitor.maxIndividuals(subjectsToDisplay.get());

          pipelineStageInfo.incrementIterationAndSetState(PipelineStageState.ITERATION_START);

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

          pipelineStageInfo.set(PipelineStageState.ITERATION_FINALIZATION_INPROGRESS);
          try {
            datastore.writePipelines(Lists.newArrayList(state()));
          } catch (DatastoreException e) {
            log.severe("can't write pipeline into datastore: " + e.getMessage());
          }

          try {
            historyDatastore.writeState(historyState());
          } catch (HistoryDatastoreException e) {
            log.severe(
                "can't write pipeline history state into history datastore: " + e.getMessage());
          }
        } finally {
          pipelineIterationScope.exit();
        }

        pipelineSynchronizer.finalizeCompleteHook();

      } while (notCompleted  && !isKilled.get());
    } catch (RuntimeException e) {
      log.log(Level.SEVERE, "Fatal error", e);
      throw e;
    }
  }

  public PipelineSynchronizer getPipelineSynchronizer() {
    return pipelineSynchronizer;
  }

  public PipelineStageInfo.ImmutablePipelineStageInfo getImmutablePipelineStageInfo() {
    return pipelineStageInfo.getImmutableValueCopy();
  }

  public DisplayMediator getDisplayableInformationProvider() {
    // TODO(sanragsood): Downcasting... DANGEROUS !
    // Once the new interface is ready, decouple DisplayMediator from Displayable and directly
    // inject DisplayMediator.
    return (DisplayMediator) displayable;
  }
}
