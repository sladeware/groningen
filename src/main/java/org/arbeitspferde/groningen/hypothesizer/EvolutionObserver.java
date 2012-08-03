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

package org.arbeitspferde.groningen.hypothesizer;

import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.PopulationData;

import java.util.List;

/**
 * Call-back interface so that programs can monitor the state of a long-running evolutionary
 * algorithm.
 *
 * @param <T> The type of entity that exists in the evolving population that is being observed.
 */
public interface EvolutionObserver<T> {
  /**
   * Invoked when the state of the population has changed (typically at the end of a generation).
   *
   * @param data Statistics about the state of the current generation.
   * @param population Evaluated population.
   */
  void populationUpdate(PopulationData<T> data, List<EvaluatedCandidate<T>> population);
}
