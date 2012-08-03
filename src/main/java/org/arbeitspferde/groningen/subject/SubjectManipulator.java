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

import org.arbeitspferde.groningen.utility.PermanentFailure;
import org.arbeitspferde.groningen.utility.TemporaryFailure;

// TODO(team): Evaluate migrating the API to a Future.
/**
 * {@link SubjectManipulator} is used to twiddle the lifecycle state of the various experimental
 * {@link Subject}s or their {@link SubjectGroup}.
 */
public interface SubjectManipulator {
  /**
   * Synchronously restart all of the subjects in a given subjec group. When this method
   * returns all of the subjects will have successfully restarted.
   *
   * @param group The group to restart
   * @param switchRateLimit Time, in milliseconds, to wait between restarting
   *        the various subjects. No argument is generated when this is set to 0.
   * @param maxWaitMillis Maximum amount of time to wait for the whole operation
   *        to complete. If this parameter is non-positive we will wait forever.
   *        Otherwise a {@link TemporaryFailure} will be thrown if the
   *        operation does not complete in the specified period of time.
   */
  public void restartGroup(SubjectGroup group, long switchRateLimit, long maxWaitMillis)
      throws PermanentFailure, TemporaryFailure;

  /**
   * Synchronously restart a given subject. When this method returns the subjects
   * will have successfully restarted.
   *
   * @param individual The subject to restart
   * @param maxWaitMillis Maximum amount of time to wait for the whole operation
   *        to complete. If this parameter is non-positive we will wait forever.
   *        Otherwise a {@link TemporaryFailure} will be thrown if the
   *        operation does not complete in the specified period of time.
   */
  public void restartIndividual(Subject individual, long maxWaitMillis)
      throws PermanentFailure, TemporaryFailure;

  /**
   * Queries for the size of a given subject group.
   *
   * @param group The subject group whose size is to be queried
   * @param maxWaitMillis Maximum amount of time to wait for the operation to
   *        complete. If this parameter is non-positive we will wait forever.
   *        Otherwise a {@link TemporaryFailure} will be thrown if the
   *        operation does not complete in the specified period of time.
   * @return The size of the given subject group or 0 if the size cannot be determined.
   */
  public int getPopulationSize(SubjectGroup group, long maxWaitMillis)
      throws PermanentFailure, TemporaryFailure;
}
