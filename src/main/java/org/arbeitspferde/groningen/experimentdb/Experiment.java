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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.logging.Logger;

/**
 * Mapping of an ID that uniquely identifies a particular experiment (generation, in GA terminology)
 * to a list of record IDs, where each record ID indicates an individual in the population. So, the
 * mapping is one to many. The number of record IDs associated with an experiment remains constant
 * between experiments, but it is not enforced here.
 *
 * This class is not thread safe.
 */
public class Experiment extends InMemoryCache.Value<Experiment> {

  private List<Long> subjectIds;

  /** Logger for this class */
  private static final Logger log =
      Logger.getLogger(Experiment.class.getCanonicalName());

  /** This experiment's database */
  private final ExperimentDb experimentDb;

  /**
   * Creates an instance of this class.
   *
   * @param experimentDb Experiment database
   * @param id Experiment id
   * @param subjectIds Subject IDs in the experiment.
   */
  Experiment(ExperimentDb experimentDb, long id, List<Long> subjectIds) {
    super(id);
    checkNotNull(experimentDb);

    this.experimentDb = experimentDb;
    setSubjectIds(subjectIds);
  }

  /**
   * Sets the subject ids on this instance
   *
   * @param subjectIds Subject IDs in the experiment.
   */
  public void setSubjectIds(List<Long> subjectIds) {
    Preconditions.checkArgument(subjectIds != null, "Record IDs cannot be null.");
    Preconditions.checkArgument(!subjectIds.isEmpty(), "Record IDs cannot be an empty list.");

    this.subjectIds = ImmutableList.copyOf(subjectIds);
  }

  /**
   * Returns the list of subject IDs associated with this experiment.
   *
   * @return Immutable list of subject IDs.
   */
  public List<Long> getSubjectIds() {
    return subjectIds;
  }

  /**
   * Returns the list of subjects associated with this experiment. Null subjects are skipped.
   *
   * @return Immutable list of {@link SubjectStateBridge}s
   */
  public List<SubjectStateBridge> getSubjects() {
    ImmutableList.Builder<SubjectStateBridge> builder = ImmutableList.builder();

    for (Long subjectId : subjectIds) {
      SubjectStateBridge subject = experimentDb.lookupSubject(subjectId);
      if (subject != null) {
        builder.add(subject);
      } else {
        log.warning("Unable to find a subject associated with the experiment");
      }
    }

    return builder.build();
  }

}
