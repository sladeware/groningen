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

/**
 * The ComputeScore interface should be used by any class that intends to
 * compute a score using data contained within a subclass of
 * {@link InMemoryCache}.
 *
 * A score is a non-negative double. It is used to compare subjects based on
 * some metric, such as 99th percentile pause time of GC phases occurring during
 * an experiment used as an application latency signal for the
 * {@link Hypothesizer}.
 *
 * A bigger score is better than a smaller score, for some definition of better.
 */
interface ComputeScore {
  /**
   * Computes a non-negative score for this subject's data. Bigger is better. It is
   * called by the {@link Hypothesizer} to compute a score for data within an
   * {@link InMemoryCache}.
   *
   * There is an {@link Enum} implemented by classes implementing this interface
   * that provides various values for scoreType. The value of scoreType
   * determines what type of score is returned.
   */
  public double computeScore(final Enum<?> scoreType);

  /** Invalidates a subject, for the purposes of computing a score this was an invalid subject. */
  public void invalidate();
}
