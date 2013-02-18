package org.arbeitspferde.groningen;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * PipelineManager is used to start new pipelines. It also does the bookkeeping, maintaining
 * the list of currently running pipelines.
 *
 * @author mbushkov@google.com (Mikhail Bushkov)
 */
@Singleton
public class PipelineManager {
  private static final Logger log = Logger.getLogger(PipelineManager.class.getCanonicalName());

  private final PipelineIdGenerator pipelineIdGenerator;
  private final BlockScope pipelineScope;
  private final Provider<Pipeline> pipelineProvider;
  private final Datastore datastore;

  private ConcurrentMap<PipelineId, Pipeline> pipelines;

  @Inject
  public PipelineManager(PipelineIdGenerator pipelineIdGenerator,
      @Named(PipelineScoped.SCOPE_NAME) BlockScope pipelineScope,
      Provider<Pipeline> pipelineProvider,
      Datastore datastore) {
    this.pipelineIdGenerator = pipelineIdGenerator;
    this.pipelineScope = pipelineScope;
    this.pipelineProvider = pipelineProvider;
    this.datastore = datastore;
    this.pipelines = new ConcurrentHashMap<PipelineId, Pipeline>();
  }

  public PipelineId restorePipeline(final PipelineState pipelineState,
      final ConfigManager configManager, final boolean blockUntilStarted) {
    final GroningenConfig firstConfig = configManager.queryConfig();
    final PipelineId pipelineId = pipelineState.pipelineId();

    final ReentrantLock pipelineConstructionLock = new ReentrantLock();
    final Condition pipelineConstructionCondition = pipelineConstructionLock.newCondition();
    // NOTE(mbushkov): using AtomicReference here is somewhat redundant. We use a dedicated lock
    // (pipelineConstructionLock) and do not require atomicity. Still it's very handy to just
    // use a reference class that is already in a standard library
    final AtomicReference<Pipeline> pipelineReference = new AtomicReference<Pipeline>();

    log.fine("starting thread for pipeline (restoring) " + pipelineId.toString());
    Thread pipelineThread = new Thread("pipeline-restore-" + pipelineId.toString()) {
      @Override
      public void run() {
        try {
          pipelineScope.enter();
          try {
            pipelineScope.seed(PipelineId.class, pipelineId);
            pipelineScope.seed(ConfigManager.class, configManager);

            Pipeline pipeline;
            pipelineConstructionLock.lock();
            try {
              pipeline = pipelineProvider.get();
              pipelineReference.set(pipeline);
              pipelines.put(pipelineId, pipeline);
              pipelineConstructionCondition.signal();
            } finally {
              pipelineConstructionLock.unlock();
            }

            log.fine("running pipeline " + pipelineId.toString());
            pipeline.restoreState(pipelineState);
            
            pipeline.run();
          } finally {
            pipelineScope.exit();
            pipelines.remove(pipelineId);

            try {
              datastore.deletePipelines(new PipelineId[] { pipelineId });
            } catch (DatastoreException e) {
              log.severe(String.format("deleting pipeline failed (pipeline id: %s): %s",
                  pipelineId.toString(), e.getMessage()));
            }
          }
        } catch (RuntimeException e) {
          log.severe(e.toString());
        }
      }
    };
    pipelineThread.start();

    if (blockUntilStarted) {
      pipelineConstructionLock.lock();
      try {
        while (pipelineReference.get() == null) {
          pipelineConstructionCondition.awaitUninterruptibly();
        }
      } finally {
        pipelineConstructionLock.unlock();
      }
    }

    return pipelineId;    
  }
  
  /**
   * Start new Pipeline with a given {@link ConfigManager}
   *
   * @param configManager {@link ConfigManager} to be used to query for pipeline's configurations
   * @param blockUntilStarted Block until pipeline's thread is actually started and the pipeline
   *                          itself is registered in the {@link PipelineManager}
   * @return {@link PipelineId} of the pipeline that is: a) about to be started
   *         (if blockUntilStarted == false) b) was started (if blockUntilStarted == true)
   */
  public PipelineId startPipeline(final ConfigManager configManager,
      final boolean blockUntilStarted) {
    final GroningenConfig firstConfig = configManager.queryConfig();
    final PipelineId pipelineId = pipelineIdGenerator.generatePipelineId(firstConfig);

    final ReentrantLock pipelineConstructionLock = new ReentrantLock();
    final Condition pipelineConstructionCondition = pipelineConstructionLock.newCondition();
    // NOTE(mbushkov): using AtomicReference here is somewhat redundant. We use a dedicated lock
    // (pipelineConstructionLock) and do not require atomicity. Still it's very handy to just
    // use a reference class that is already in a standard library
    final AtomicReference<Pipeline> pipelineReference = new AtomicReference<Pipeline>();

    log.fine("starting thread for pipeline " + pipelineId.toString());
    Thread pipelineThread = new Thread("pipeline-" + pipelineId.toString()) {
      @Override
      public void run() {
        try {
          pipelineScope.enter();
          try {
            pipelineScope.seed(PipelineId.class, pipelineId);
            pipelineScope.seed(ConfigManager.class, configManager);

            Pipeline pipeline;
            pipelineConstructionLock.lock();
            try {
              pipeline = pipelineProvider.get();
              pipelineReference.set(pipeline);
              pipelines.put(pipelineId, pipeline);
              pipelineConstructionCondition.signal();
            } finally {
              pipelineConstructionLock.unlock();
            }

            log.fine("writing to datastore " + pipelineId.toString());
            try {
              datastore.createPipeline(pipeline.state(), /* checkForConflicts */ true);
            } catch (DatastoreException e) {
              log.severe(String.format("writing to datastore failed (pipeline id: %s): %s",
                  pipelineId.toString(), e.getMessage()));
            }
            
            log.fine("running pipeline " + pipelineId.toString());
            pipeline.run();            
          } finally {
            pipelineScope.exit();
            pipelines.remove(pipelineId);
            
            try {
              datastore.deletePipelines(new PipelineId[] { pipelineId });
            } catch (DatastoreException e) {
              log.severe(String.format("deleting pipeline failed (pipeline id: %s): %s",
                  pipelineId.toString(), e.getMessage()));
            }
          }
        } catch (RuntimeException e) {
          log.severe(e.toString());
        }
      }
    };
    pipelineThread.start();

    if (blockUntilStarted) {
      pipelineConstructionLock.lock();
      try {
        while (pipelineReference.get() == null) {
          pipelineConstructionCondition.awaitUninterruptibly();
        }
      } finally {
        pipelineConstructionLock.unlock();
      }
    }

    return pipelineId;
  }

  /**
   * @param pipelineId {@link PipelineId} identifying the needed pipeline
   * @return {@link Pipeline} corresponding to current PipelineId or null if such pipeline was
   *         not ever created or is already dead
   */
  public Pipeline findPipelineById(PipelineId pipelineId) {
    return pipelines.get(pipelineId);
  }

  /**
   * Returns the snapshot of the list of all currently running pipelines
   *
   * @return Map of PipelineId->Pipeline
   */
  public Map<PipelineId, Pipeline> getAllPipelines() {
    return new HashMap<PipelineId, Pipeline>(pipelines);
  }
}
