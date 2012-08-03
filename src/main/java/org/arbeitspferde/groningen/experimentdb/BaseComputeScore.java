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

package org.arbeitspferde.groningen.experimentdb;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base implementation of {@link ComputeScore} for shared functionality.
 */
abstract class BaseComputeScore implements ComputeScore {
  /** Whether or not this is a valid score */
  private boolean invalid = false;

  @Override
  public void invalidate() {
    this.invalid = true;
  }

  @Override
  public double computeScore(Enum scoreType) {
    checkNotNull(scoreType);
    if (this.invalid) {
      return 0.0;
    } else {
      return computeScoreImpl(scoreType);
    }
  }

  /** Per-metric implementation of score. Can assume score is valid. */
  protected abstract double computeScoreImpl(Enum scoreType);

  /** Call this method when handling an invalid score type in computeScore() */
  protected double handleInvalidScoreType() {
    throw new UnsupportedOperationException("Undefined score type.");
  }
}
