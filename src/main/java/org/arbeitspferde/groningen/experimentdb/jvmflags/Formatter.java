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
 * Provide standardized representation and validation of flags.
 *
 * This interface subscribes to the flyweight pattern, which allows for
 * stateless singletons to exist which perform work situationally for the
 * concrete reference instances they refer to.
 */
interface Formatter {
  /**
   * Provide a representation of the JVM flag with a value as a string.
   *
   * @param cla The argument for which this formatter is used.
   * @param value The value that the flag shall be set to.
   * @return The emitted formatted argument string.
   */
  String asArgumentString(final JvmFlag cla, final long value);

  /**
   * Provide a representation of the flag with possible values as a reg. exp.
   *
   * @param cla The argument for which this formatter is used.
   * @return The regular expression representation.
   */
  String asRegularExpressionString(final JvmFlag cla);

  /**
   * Provide a representation of the acceptable values the flag may be set to.
   *
   * @param cla The argument for which this formatter is used.
   * @return The valid value representation.
   */
  String asAcceptableValuesString(final JvmFlag cla);

  /**
   * Verify that the proposed value is acceptable.
   *
   * Implementors should include information in the thrown exception about why
   * proposedValue is unacceptable.
   *
   * @param cla The argument for which this formatter is used.
   * @param proposedValue The value to verify.
   * @throws IllegalArgumentException if proposedValue is unacceptable.
   */
  void validate(final JvmFlag cla, final long proposedValue) throws IllegalArgumentException;
}
