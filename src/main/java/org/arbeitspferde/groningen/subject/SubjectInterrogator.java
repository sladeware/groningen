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

/**
 * SubjectInterrogator is responsible for acquiring information about various aspects of a running
 * experimental subject's state.
 */
public interface SubjectInterrogator {
  /**
   * Returns the last time that the given subject restarted, or {@code null}
   * if there is a problem contacting the server.
   *
   * @param subject
   * @return a {@link String} representation of a long representing last restart
   *         time expressed as a number of seconds since the epoch
   * @throw {@link SubjectInterrogationException} in case the given information cannot be retrieved.
   */
  public String getLastSubjectRestartTime(final Subject subject)
      throws SubjectInterrogationException;

  /**
   * Returns the command-line string that was used to start the given experimental subject.
   *
   * @param subject Subject whose command-line string needs to be extracted.
   * @return Command-line string.
   * @throw {@link SubjectInterrogationException} in case the given information cannot be retrieved.
   */
  public String getCommandLine(final Subject subject) throws SubjectInterrogationException;
}
