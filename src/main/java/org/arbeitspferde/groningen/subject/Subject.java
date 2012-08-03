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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class represents a single individual that is undergoing experimentation.
 */
public class Subject {

  /** The {@link SubjectGroup} that this is part of */
  private final SubjectGroup subjectGroup;

  /** The population (i.e., {@link SubjectGroup}) identification index of the {@link Subject}. */
  private final int subjectIndex;

  /**
   * The name of the experiment settings file to relay settings to this subject when it is
   * restarted by the {@link Executor}.
   */
  private final String expSettingsFile;

  private final String servingAddress;

  public Subject(final SubjectGroup group, final String expSettingsFile, final int subjectIndex,
      final ServingAddressGenerator servingAddressBuilder) {
    Preconditions.checkNotNull(group);
    Preconditions.checkArgument(subjectIndex >= 0, "Subject index must be non-negative");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(expSettingsFile),
        "experiment settings file path cannot be null or empty");

    this.subjectGroup = group;
    this.subjectIndex = subjectIndex;
    this.expSettingsFile = expSettingsFile;
    this.servingAddress = servingAddressBuilder.addressFor(group, subjectIndex);
  }

  @Override
  public String toString() {
    return String.format("[Subject index %s of group %s]", subjectIndex, subjectGroup);
  }

  public String getServingAddress() {
    return servingAddress;
  }

  /**
   * @return The name of the experiment settings file to relay settings to this subject when it is
   * restarted by the {@link Executor}.
   */
  public String getExpSettingsFile() {
    return expSettingsFile;
  }

  public int getSubjectIndex() {
    return subjectIndex;
  }

  /**
   * @return The subject group this subject belongs to.
   */
  public SubjectGroup getSubjectGroup() {
    return subjectGroup;
  }

  /** Returns the number of seconds a subject is permitted to fail health checking. */
  public long getWarmupTimeout() {
    return subjectGroup.getSubjectGroupConfig().getSubjectWarmupTimeout();
  }
}
