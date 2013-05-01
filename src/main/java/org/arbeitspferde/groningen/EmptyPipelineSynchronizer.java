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
 * A {@link PipelineSynchronizer} that passes through for each method allowing the pipeline to
 * operate without external influence.
 */
public class EmptyPipelineSynchronizer implements PipelineSynchronizer {

  /** @see PipelineSynchronizer#supportsSyncPoints(org.arbeitspferde.groningen.SyncPoint[]) */
  @Override
  public boolean supportsSyncPoints(SyncPoint... points) {
    if (points.length > 0) {
      return false;
    }
    return true;
  }

  // Pipeline Hooks
  public EmptyPipelineSynchronizer() {}

  @Override
  public void iterationStartHook() {}

  @Override
  public void executorStartHook() {}

  @Override
  public void initialSubjectRestartCompleteHook() {}

  @Override
  public boolean shouldFinalizeExperiment() {
    return false;
  }

  @Override
  public void finalizeCompleteHook() {}
  
  // Sync Points
  @Override
  public boolean blockTilIterationStart(long maxWaitSecs) throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public void allowPastIterationStart() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public boolean blockTilExperimentArgsPushed(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public void allowPastExperimentArgsPushed() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public boolean blockTilRestartedWithExpArgs(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public void allowPastRestartedWithExpArgs() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public void flagEndOfIteration() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public boolean blockTilIterationFinalization(long maxWaitSecs)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }

  @Override
  public void allowPastIterationFinalization() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        this.getClass().getEnclosingMethod().getName() + " implements no sync points");
  }
}
