package org.arbeitspferde.groningen;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A pipeline synchronizer that only supports one point of synchronization: blocking the pipeline
 * at the end of an iteration until it is signaled. The signal can arrive before the pipeline
 * arrives at the end of the iteration blocking point. Such a blocking pattern is meant to provide
 * clients a means to analyze the scores and decide if another iteration is warranted.
 */
public class IterationFinalizationSynchronizer implements PipelineSynchronizer {

  /** Sync points this synchronizer supports */
  private static final SyncPoint.ComposedSyncPoints supportedSyncPoints =
      new SyncPoint.ComposedSyncPoints(SyncPoint.SIGNAL_ITERATION_FINALIZATION);

  // Locks and condition to support a single point of synchronization
  private final Lock finalizerLock = new ReentrantLock();
  private final Condition finalizerCondition = finalizerLock.newCondition();
  
  // Flag to allow one to give a pass to the pipeline before it arrives at the
  // IterationFinalizationSync point
  private boolean finalizedSinceIterationBegin = false;
  
  private PipelineStageInfo pipelineStageInfo = null;
  
  /** @see PipelineSynchronizer#supportsSyncPoints(SyncPoint[]) */
  @Override
  public boolean supportsSyncPoints(SyncPoint... points) {
    return supportedSyncPoints.supportsSyncPoints(points);
  }
  
  /** @see PipelineSynchronizer#setPipelineStageTracker(PipelineStageInfo)
   */
  @Override
  public void setPipelineStageTracker(PipelineStageInfo pipelineStageInfo) {
    this.pipelineStageInfo = pipelineStageInfo;
  }


  /**
   * @see PipelineSynchronizer#iterationStartHook()
   * 
   * Called here to reset the finalization flag at the beginning of the iteration.
   */
  @Override
  public void iterationStartHook() {
    finalizerLock.lock();
    try {
      finalizedSinceIterationBegin = false;      
    } finally {
      finalizerLock.unlock();
    }
  }

  /**
   * @see org.arbeitspferde.groningen.PipelineSynchronizer#finalizeCompleteHook()
   * 
   * Block the pipeline until the client steps the finalization flag or the condition is signaled.
   */
  @Override
  public void finalizeCompleteHook() {
    finalizerLock.lock();
    try {
      if (!finalizedSinceIterationBegin) {
        if (pipelineStageInfo != null) {
          pipelineStageInfo.set(PipelineStageState.ITERATION_FINALIZED_WAIT);
        }

        // TODO(etheon): revisit being interrupted, its meaning, how to use it in the restart of
        // an iteration or pausing...
        finalizerCondition.awaitUninterruptibly();
      }
    } finally {
      finalizerLock.unlock();
    }
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#allowPastIterationFinalization() */
  @Override
  public void allowPastIterationFinalization() throws UnsupportedOperationException {
    finalizerLock.lock();
    try {
      finalizedSinceIterationBegin = true;
      finalizerCondition.signal();
    } finally {
      finalizerLock.unlock();
    }
  }

  // Most hooks have nothing to do
  /** @see PipelineSynchronizer#executorStartHook() */
  @Override
  public void executorStartHook() {}

  /** @see PipelineSynchronizer#initialSubjectRestartCompleteHook() */
  @Override
  public void initialSubjectRestartCompleteHook() {}

  /** @see PipelineSynchronizer#shouldFinalizeExperiment() */
  @Override
  public boolean shouldFinalizeExperiment() {
    return false;
  }

  // The rest of the sync points are unused too
  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#blockTilIterationStart(long) */
  @Override
  public boolean blockTilIterationStart(long maxWaitSecs) throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#allowPastIterationStart() */
  @Override
  public void allowPastIterationStart() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#blockTilExperimentArgsPushed(long) */
  @Override
  public boolean blockTilExperimentArgsPushed(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#allowPastExperimentArgsPushed() */
  @Override
  public void allowPastExperimentArgsPushed() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#blockTilRestartedWithExpArgs(long) */
  @Override
  public boolean blockTilRestartedWithExpArgs(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#allowPastRestartedWithExpArgs() */
  @Override
  public void allowPastRestartedWithExpArgs() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#flagEndOfIteration() */
  @Override
  public void flagEndOfIteration() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }

  /** @see org.arbeitspferde.groningen.PipelineSynchronizer#blockTilIterationFinalization(long) */
  @Override
  public boolean blockTilIterationFinalization(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " is not used in "
        + this.getClass().getName());    
  }
}
