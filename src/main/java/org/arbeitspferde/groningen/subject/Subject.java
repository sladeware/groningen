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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class represents a single individual that is undergoing experimentation.
 */
public class Subject {

  /** The {@link SubjectGroup} that this is part of */
  private final SubjectGroup group;

  /** The population (i.e., {@link SubjectGroup}) identification index of the {@link Subject}. */
  private final int index;

  /** Indicates whether or not this is a default subject. */
  private final boolean isDefault;

  /**
   * The name of the experiment settings file to relay settings to this subject when it is
   * restarted by the {@link Executor}.
   */
  private final String expSettingsFile;

  private final String servingAddress;

  public Subject(final SubjectGroup group, final String expSettingsFile, final int index,
      final ServingAddressGenerator servingAddressBuilder) {
    this(group, expSettingsFile, index, servingAddressBuilder, false);
  }

  public Subject(final SubjectGroup group, final String expSettingsFile, final int index,
      final ServingAddressGenerator servingAddressBuilder, boolean isDefault) {
    Preconditions.checkNotNull(group);
    Preconditions.checkArgument(index >= 0, "Subject index must be non-negative");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(expSettingsFile),
        "experiment settings file path cannot be null or empty");

    this.group = group;
    this.index = index;
    this.expSettingsFile = expSettingsFile;
    this.servingAddress = servingAddressBuilder.addressFor(group, index);
    this.isDefault = isDefault;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Subject.class)
        .add("index", index)
        .add("group", group)
        .toString();
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

  public int getIndex() {
    return index;
  }

  /**
   * @return The subject group this subject belongs to.
   */
  public SubjectGroup getGroup() {
    return group;
  }

  /** Returns the number of seconds a subject is permitted to fail health checking. */
  public long getWarmupTimeout() {
    return group.getSubjectGroupConfig().getSubjectWarmupTimeout();
  }

  public boolean isDefault() {
    return isDefault;
  }
}
