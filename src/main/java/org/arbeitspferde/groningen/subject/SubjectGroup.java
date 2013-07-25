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

package org.arbeitspferde.groningen.subject;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.PermanentFailure;
import org.arbeitspferde.groningen.utility.TemporaryFailure;

import java.util.List;

/**
 * This class represents an experimental population of {@link Subject}s.
 */
public class SubjectGroup {
  private static final Joiner slashJoiner = Joiner.on("/");
  private static final Joiner commaJoiner = Joiner.on(",");

  @Inject
  @NamedConfigParam("subject_manipulation_deadline_ms")
  private int populationCensusCollectionDeadline =
      GroningenParams.getDefaultInstance().getSubjectManipulationDeadlineMs();

  private final String clusterName;
  private final String subjectGroupName;
  private final String userName;
  private final SubjectGroupConfig config;
  private final List<Subject> subjects;
  private final ServingAddressGenerator servingAddressbuilder;

  public SubjectGroup(final String clusterName, final String subjectGroupName,
      final String userName, final SubjectGroupConfig groupConfig,
      final ServingAddressGenerator servingAddressBuilder) {
    this.clusterName = clusterName;
    this.subjectGroupName = subjectGroupName;
    this.userName = userName;
    this.config = groupConfig;
    this.subjects = Lists.newArrayList();
    this.servingAddressbuilder = servingAddressBuilder;
  }

  /** Initializes subject data
   *
   * You must call initialize before accessing any of the subject data or you won't find any :)
   *
   * @param manipulator The means for manipulating subject state.
   * @param origSubjectList The original subject list that the new list is concatenated with.
   *                        This has the side effect of concatinating the new list onto the
   *                        origSubjectList.
   *
   * @return The list of subjects within this subject we control concatenated onto the origSubjectList
   */
  public List<Subject> initialize(SubjectManipulator manipulator, List<Subject> origSubjectList)
      throws PermanentFailure, TemporaryFailure {
    int subjectCount = manipulator.getPopulationSize(this, populationCensusCollectionDeadline);
    if ((config.getNumberOfSubjects() > 0) && (config.getNumberOfSubjects() < subjectCount)) {
      subjectCount = config.getNumberOfSubjects();
    }
    int index;
    for (index = 0; index < subjectCount - config.getNumberOfDefaultSubjects();
         index++) {
      Subject subject = new Subject(this, getSubjectConfiguration(index), index,
                                    servingAddressbuilder);
      subjects.add(subject);
    }
    for (; index < subjectCount; index++) {
      Subject subject = new Subject(this, getSubjectConfiguration(index), index,
                                    servingAddressbuilder, true);
      subjects.add(subject);
    }
    if (origSubjectList != null) {
      origSubjectList.addAll(subjects);
      return origSubjectList;
    } else {
      return subjects;
    }
  }

  /** Initializes subject data
   *
   * You must call initialize before accessing any of the subject data or you won't find any :)
   *
   * @param manipulator The means for manipulating subject lifecycle.
   *
   * @return The list of subjects within this group that we control
   */
  public List<Subject> initialize(SubjectManipulator manipulator)
      throws PermanentFailure, TemporaryFailure {
    return initialize(manipulator, null);
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getSubjectGroupName() {
    return subjectGroupName;
  }

  public String getUserName() {
    return userName;
  }

  /** Get a concatenated list of subjects indices
   *
   * Returns a comma separated string of subject numbers under our control or an empty string if
   * there are none.
   */
  public String getSubjectIndicesConcated() {
    if (getSubjectCount() == 0) {
      return "";
    }

    String subjectIndices = null;
    for (Subject subject : subjects) {
      subjectIndices = commaJoiner.skipNulls().join(subjectIndices,
          Integer.toString(subject.getSubjectIndex()));
    }

    return subjectIndices;
  }

  /** Get the number of subjects in the subject group we are allowed to control */
  public int getSubjectCount() {
    return subjects.size();
  }

  @Override
  public String toString() {
    return String.format("[Subject Group %s/%s in %s]", subjectGroupName, userName, clusterName);
  }

  public SubjectGroupConfig getSubjectGroupConfig() {
    return config;
  }

  private String getSubjectConfiguration(final int index) {
    return slashJoiner.join(config.getExperimentSettingsFilesDir(), index);
  }
}
