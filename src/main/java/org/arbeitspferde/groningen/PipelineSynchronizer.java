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
 */
public interface PipelineSynchronizer {
  /**
   * Hook for synchronization before the Hypothesizer is run.
   *
   * This method can block.
   */
  public void iterationStartHook();

  /**
   * Hook for synchronization between the Generator and Executor.
   *
   * This method can block.
   */
  public void executorStartHook();

  /**
   * Hook for signaling that tasks have been restarted with the experimental arguments.
   *
   * This method can block but is not expected to.
   */
  public void initialSubjectRestartCompleteHook();

  /**
   * Polling method for external notification that the executor should complete the experiment.
   * This is not notification of an external error condition, instead, it is external signaling
   * that the experiment has concluded.
   *
   * This method <b>CANNOT<b> block. It is meant to be polled.
   *
   * @return boolean true iff the executor should finalize this experiment
   */
  public boolean shouldFinalizeExperiment();

  /**
   * Hook for synchronization after the final Extractor has run in this iteration.
   *
   * This method can block.
   */
  public void finalizeCompleteHook();
}
