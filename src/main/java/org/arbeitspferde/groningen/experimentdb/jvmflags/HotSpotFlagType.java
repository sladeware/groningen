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
 * A representation of JVM flag categories.
 *
 * http://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html.
 */
enum HotSpotFlagType {
  /**
   * Non-standard flag.
   */
  NON_STANDARD("-X"),
  /**
   * Unstable flag.
   */
  UNSTABLE("-XX:");

  private final String prefix;

  private HotSpotFlagType(final String prefix) {
    this.prefix = prefix;
  }

  /**
   * Yield the appropriate flag prefix.
   */
  public String getPrefix() {
    return prefix;
  }
}
