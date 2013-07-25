/* Copyright 2013 Google, Inc.
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

package org.arbeitspferde.groningen.subject.open;

import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.subject.HealthQuerier;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.utility.Process;

/**
 * This class provides a way to check the health of a process. It checks whether or not a process
 * with the specified PID is running.
 */
@PipelineIterationScoped
public class ProcessHealthQuerier implements HealthQuerier {

  /**
   * Wait for the subject to come online and mark itself as healthy.
   *
   * @return {@code true} if the subject is online and healthy within a given
   *         implementation-specific deadline; {@code false} otherwise.
   */
  @Override
  public boolean blockUntilHealthy(Subject subject) {
    return true;
  }

  /**
   * @return {@code true} if the subject is healthy; {@code false} otherwise.
   */
  @Override
  public boolean isHealthy(Subject subject) {
    ProcessServingAddressGenerator.ProcessServingAddressInfo addressInfo =
        ProcessServingAddressGenerator.parseAddress(subject.getServingAddress());
    if (addressInfo == null) {
      return false;
    }
    // TODO(drk): support named group identifiers.
    int processGroupId;
    try {
      processGroupId = Integer.parseInt(addressInfo.getProcessGroupName());
    } catch (NumberFormatException e) {
      return false;
    }
    Process.ProcessInfo processInfo = Process.getProcessInfo(addressInfo.getProcessId());
    // Define whether the process was killed or another process took its PID.
    // TODO(drk): add user name check.
    if (processInfo == null || processInfo.getProcessGroupId() != processGroupId) {
      return false;
    }
    return Process.isAlive(processInfo.getProcessId());
  }
}