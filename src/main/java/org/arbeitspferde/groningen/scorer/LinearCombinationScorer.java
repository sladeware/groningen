/* Copyright 2013 Google, Inc.
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

package org.arbeitspferde.groningen.scorer;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.FitnessScore;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;

/**
 * A {@link SubjectStateBridge} scorer that calculates a linear combination of metric scores.
 * 
 * Weights for the metrics can be specified by the user within the param block in the
 * @{link GroningenConfig}.
 */
public class LinearCombinationScorer implements SubjectScorer {

  /** @see SubjectScorer#compute(SubjectStateBridge, GroningenConfig) */
  @Override
  public double compute(SubjectStateBridge subject, GroningenConfig config) {
    // TODO(etheon): Move guts of FitnessScore here after refactoring HistoryDataStore scoring
    // additions
    return FitnessScore.compute(subject, config);
  }
}
