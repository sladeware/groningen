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

import org.arbeitspferde.groningen.common.EvaluatedSubject;

import java.util.List;

/**
 * Ways to calculate, store, and retrieve the best performers over the life of a pipeline.
 *
 * Implementations are expected to be reentrant and modifications to the best performer lists
 * should be atomic.
 */
public interface HistoricalBestPerformerScorer {
  /**
   * Add another generation of {@link EvaluatedSubject EvaluatedSubjects} to the historical
   * store.
   *
   * The implementation will determine how to deal with subjects that test the same
   * arguments over different generations. Implementations should not modify the list
   * passed in - any and all modifications should be made on duplicates of the list.
   *
   * EvaluatedSubjects, which has be considered owned by the calling code, shall not be modified
   * within the scorer without first duplicating.
   *
   * @param newGeneration the list of EvaluatedSubjects to add to the store.
   * @returns a list representing the processed generation which might include combining scores
   *    for subjects representing the same command line. This copy of the list is considered
   *    owned by the caller. It is up to the implementation as to whether this list will be
   *    sorted.
   */
  public List<EvaluatedSubject> addGeneration(List<EvaluatedSubject> newGeneration);

  /**
   * Retrieve a ranked list of all EvaluatedSubjects over the life of the experimental pipeline.
   *
   * The scorer may concatenate results from subjects in ways it sees fit (for instance
   * averaging or taking the max of subjects testing with the same command line in different
   * generations).
   *
   * @return a list ordered from best to worst of EvaluatedSubjects for all subjects in the
   *    life off the pipeline
   */
  public List<EvaluatedSubject> getBestPerformers();

  /**
   * Retrieve the head end of the ranked list of best performers over the life of the pipeline.
   *
   * The scorer may concatenate results from subjects in ways it sees fit (for instance
   * averaging or taking the max of subjects testing with the same command line in different
   * generations).
   *
   * @param maxEntries max number of entries from the list to return
   * @return a list ordered from best to worst of EvaluatedSubjects for all subjects in the
   *    life off the pipeline
   */
  public List<EvaluatedSubject> getBestPerformers(int maxEntries);
}
