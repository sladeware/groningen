package org.arbeitspferde.groningen;

/**
 * Stages a {@link Pipeline} can be in. Not all states will be transitioned by all
 * Pipelines as many states occur (or are triggered) by methods within
 * {@link PipelineSynchronizer PipelineSynchronizers}.
 */
public enum PipelineStageState {
  /** The Pipeline has been Initialized and nothing more. */
  INITIALIZED(),
  /** The Pipeline is at the beginning of an iteration. */
  ITERATION_START(),
  /**
   * The Pipeline is blocked in {@link PipelineSynchronizer#iterationStartHook} waiting 
   * for the condition to allow it to proceed.
   */
  ITERATION_START_WAIT(),
  /** The Pipeline is in the Hypothesizer. */
  HYPOTHESIZER(),
  /** The Pipeline is in the Generator. */
  GENERATOR(),
  /**
   * The Pipeline is blocked in {@link PipelineSynchronizer#executorStartHook} waiting 
   * for the condition to allow it to proceed.
   */
  EXECUTOR_START_WAIT(),
  /**
   * The Pipeline is in the Executor performing the initial restart of the tasks to
   * pick up the experimental arguments.
   */
  INITIAL_TASK_RESTART(),
  /**
   * The Pipeline is blocked in {@link PipelineSynchronizer#executorStartHook} waiting 
   * for the condition to allow it to proceed.
   */
  INITIAL_TASK_RESTART_COMPLETE_WAIT(),
  /** The Pipeline is in the Executor main body. */
  EXECUTOR_MAIN(),
  /** The end of the iteration has been flagged. */
  END_ITERATION_FLAGGED(),
  /** The Pipeline is removing the experimental arguments. */
  REMOVING_EXPERIMENTAL_ARGUMENTS(),
  /**
   * The Pipeline is restarting tasks to return the tasks to default arguments and roll the gc
   * logs over.
   */
  FINAL_TASK_RESTART(),
  /** The Pipeline is calculating scores */
  SCORING(),
  /** The Pipeline is paused and the tasks have been returned to their default arguments. */
  ITERATION_PAUSED(),
  ITERATION_ERROR_STATE(),
  /**
   * The Pipeline is blocked in {@link PipelineSynchronizer#finalizeCompleteHook} waiting 
   * for the condition to allow it to proceed.
   */
  ITERATION_FINALIZED_WAIT(),
  /** The iteration is being finalized and state recorded */
  ITERATION_FINALIZATION_INPROGRESS(),
  /**
   * The Experiment Pipeline is marked as completed and being torn down and committed to the
   * historical datastore.
   */
  PIPELINE_FINALIZATION_INPROGRESS(),
  /** The Pipeline has been finalized */
  PIPELINE_FINALIZED()
}
