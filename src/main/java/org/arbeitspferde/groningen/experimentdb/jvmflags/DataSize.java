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

package org.arbeitspferde.groningen.experimentdb.jvmflags;

/**
 * A set of scalar unit sizes associated with JVM flags.
 *
 * These suffices must be conformant with that which the JVM supports.  For
 * instance, it has no formal support for units in terabytes, so they cannot
 * be included in here.
 */
enum DataSize {
  /**
   * No data size is associated with these values.
   */
  NONE(""),

  /**
   * The units are in bytes.
   */
  BYTE("b"),

  /**
   * The units are in kilobytes.
   */
  KILO("k"),

  /**
   * The units are in megabytes.
   */
  MEGA("m"),

  /**
   * The units are in gigabytes.
   */
  GIGA("g");

  private static final String DATASIZE_SUFFIX_REGEXP;

  /**
   * The textual suffix associated with the unit.
   */
  private final String suffix;

  private DataSize(final String suffix) {
    this.suffix = suffix;
  }

  /**
   * Yield the appropriate suffix.
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Yield a regular expression representation of this unit.
   *
   * @return The String representation of this unit as a regular expression.
   */
  private String suffixToRegexpString() {
    return String.format("%s%s", suffix.toLowerCase(), suffix.toUpperCase());
  }

  /**
   * Represent this unit and associated units as a regular expression.
   *
   * @return The String representation of this unit and family.
   */
  public String unitFamilyAsRegexpString() {
    if (NONE.equals(this)) {
      return "";
    } else {
      return DATASIZE_SUFFIX_REGEXP;
    }
  }

  static {
    final StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (final DataSize dataSize : values()) {
      builder.append(dataSize.suffixToRegexpString());
    }
    builder.append("]");

    DATASIZE_SUFFIX_REGEXP = builder.toString();
  }
}
