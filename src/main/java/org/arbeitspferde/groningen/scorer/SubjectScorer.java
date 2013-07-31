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
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;

/**
 * Subject score calculators.
 * 
 * The score serves two functions:
 * <ul><li>Gives the hypothesizer a way to determine highest performing individuals for use it
 *      producing the next experiment's individuals.
 * </li><li>Provides the user a way to determine the best arguments from a given generation.
 * </li></ul>
 */
public interface SubjectScorer {
  /**
   * Calculate the score for a given subject.
   * <p>
   * From the information available in a @{link SubjectStateBridge} and the current
   * @{link GroningenConfig}, compose a score in the form of a double. Higher scores are
   * treated as better performers.
   * 
   * @param subject the subject to score. It is expected to be complete and validated.
   * @param config the current configuration.
   * @return the resultant fitness score.
   */
  public double compute(SubjectStateBridge subject, GroningenConfig config);
}