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
 * A means for determining whether an experimental subject is healthy.
 */
public interface HealthQuerier {

  /**
  * Wait for the subject to come online and mark itself as healthy.
  *
  * @return true if the subject is online and healthy within a given implementation-specific
  *         deadline; false otherwise.
  */
  public boolean blockUntilHealthy(final Subject subject);

  /**
   * @return true if the subject is healthy; false otherwise.
   */
  public boolean isHealthy(final Subject subject);
}
