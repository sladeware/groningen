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

/**
 * Provider of synchronization points within the Groningen processing pipeline to allow the
 * pipeline to synchronize with other agents, most likely external to the process.
 *
 * Most methods are expected to be able to block.
 * 
 * The interface provides hooks for the pipeline to call into and mechanisms to signal a
 * pipeline waiting within the hooks. These can be used to block on/receive notification that the
 * iteration has progressed to/through the points laid out below. The sync points and steps within
 * the pipeline are enumerated below.
 * 
 * Steps in the iteration are preceeded with 'step: ' while sync points are preceeded
 * by 'sync: '
 * 
 *  sync: IterationStartSync
 *  step: Hypothesizer
 *  step: ExperimentalArgumentPush
 *  sync: ExperimentalArgPreRestartSync
 *  step: InitialRestart
 *  sync: InitialRestartCompleteSync
 *  step: TasksWatched
 *  sync: EndGenerationFlagged: the iteration is flagged to be completed, less of a direct
 *        synchronization point than advisoratory.
 *  step: ExperimentalArgumentsCleared
 *  step: FinalRestart
 *  step: ScoresAssessed
 *  step: ScoringComplete
 *  sync: IterationFinalizedSync
 *  
 * The interface provides methods through which combinations of these synchronization
 * points can be built into ways to synchronize the pipeline with external applications and/or
 * conditions.
 * 
 * Pipeline most pipeline hooks are expected to block.
 */
public interface PipelineSynchronizer {
  /**
   * Expose which synchronization points are supported by a {@link PipelineSynchronizer} such
   * that methods calling into a synchronizer can verify all operatons are available before
   * starting a sequence of synchronization steps
   * 
   * @param points a variable number/array of sync points to check against
   * @return true iff all requested SyncPoints are supported by the synchronizer
   */
  public boolean supportsSyncPoints(SyncPoint ... points);
  
  /**
   * Take a reference to the PipelineStageInfo that should be updated as the API enters
   * and exits wait states.
   * 
   * @param pipelineStageInfo the state tracking object
   */
  public void setPipelineStageTracker(PipelineStageInfo pipelineStageInfo);

  /**
   * Pipeline hook for synchronization at IterationStartSync, before the Hypothesizer is run.
   *
   * This method can block.
   */
  public void iterationStartHook();

  /**
   * Pipeline hook for synchronization at ExperimentalArgPreRestartSync, between the Generator
   * and Executor.
   *
   * This method can block.
   */
  public void executorStartHook();

  /**
   * Pipeline hook for synchronization at InitialRestartCompleteSync, signaling that tasks have
   * been restarted with the experimental arguments.
   *
   * This method can block but is not expected to.
   */
  public void initialSubjectRestartCompleteHook();

  /**
   * Pipeline polling method for external notification that the executor should complete the
   * experiment, and occurs EndGenerationFlagged in the iteration progression laid out above.
   * This is not notification of an external error condition, instead, it is external signaling
   * that the experiment has concluded.
   *
   * This method <b>CANNOT<b> block. It is meant to be polled.
   *
   * @return boolean true iff the executor should finalize this experiment
   */
  public boolean shouldFinalizeExperiment();

  /**
   * Pipeline hook for synchronization at IterationFinalizedSync, after the final Extractor has
   * run in this iteration.
   *
   * This method can block.
   */
  public void finalizeCompleteHook();
  
  /**
   * Wait for the pipeline to arrive at IterationStartSync sync point with an optional time to wait.
   *
   * @params maxWaitSecs max time to wait in seconds, <= 0 will use an unbounded wait
   * @returns true iff the wait was fulfilled by the pipeline reaching the point before the
   *          specified wait time passed.
   */
  boolean blockTilIterationStart(long maxWaitSecs) throws UnsupportedOperationException;

  /**
   * Signal the pipeline that the iteration can commence (ie progress past IterationStartSync).
   */
  void allowPastIterationStart() throws UnsupportedOperationException;

  /**
   * Wait for the pipeline to finish writing the the experimental argument files (or block at
   * the ExperimentalArgPreRestartSync sync point).
   *
   * @params maxWaitSecs max time to wait in seconds, <= 0 will use an unbounded wait
   * @returns true iff the wait was fulfilled by the pipeline reaching the point before the
   *          specified wait time passed.
   */
  boolean blockTilExperimentArgsPushed(long maxWaitSecs)
      throws UnsupportedOperationException;

  /**
   * Signal the pipeline that the iteration can proceed past the writing of the experimental arg
   * files which corresponds to the ExperimentalArgPreRestartSync sync point..
   */
  void allowPastExperimentArgsPushed() throws UnsupportedOperationException;

  /**
   * Wait for the pipeline to finish restarting the tasks with experimental arguments
   * (InitialRestartCompleteSync sync point). 
   *
   * @params maxWaitSecs max time to wait in seconds, <= 0 will use an unbounded wait
   * @returns true iff the wait was fulfilled by the pipeline reaching the point before the
   *          specified wait time passed.
   */
  boolean blockTilRestartedWithExpArgs(long maxWaitSecs)
      throws UnsupportedOperationException;
  
  /**
   * Signal the pipeline that it can proceed past the InitialRestartCompleteSync sync point. 
   */
  void allowPastRestartedWithExpArgs() throws UnsupportedOperationException;

  /**
   * Signal the pipeline to begin to finish the iteration by restarting the tasks and scoring the
   * individual tasks. Brings the iteration to the EndGenerationFlagged sync point.
   */
  void flagEndOfIteration() throws UnsupportedOperationException;
  
  /**
   * Wait for the pipeline to finish scoring tasks, in other words wait for the pipeline to
   * arrive at the IterationFinalizedSync sync point. 
   *
   * @params maxWaitSecs max time to wait in seconds, <= 0 will use an unbounded wait
   * @returns true iff the wait was fulfilled by the pipeline reaching the point before the
   *          specified wait time passed.
   */
  boolean blockTilIterationFinalization(long maxWaitSecs)
      throws UnsupportedOperationException;

  /**
   * Signal the pipeline that the iteration can finalize the iteration and start another by
   * proceeding past IterationFinalizedSync. 
   */
  void allowPastIterationFinalization() throws UnsupportedOperationException;
}
