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
 * Represent the differences in JVM flag value infix delimitering.
 */
enum ValueSeparator {
  /**
   * No delimiter is used.
   */
  NONE(""),

  /**
   * An equal sign is used.
   */
  EQUAL("=");

  /**
   * The {@link Joiner} that handles the delimitering.
   */
  private final String infix;

  private ValueSeparator(final String infix) {
    this.infix = infix;
  }

  String getInfix() {
    return infix;
  }
}
