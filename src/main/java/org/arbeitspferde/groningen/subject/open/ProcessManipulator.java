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

import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.subject.SubjectManipulator;
import org.arbeitspferde.groningen.utility.Process;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class encapsulates JTune's access to the system's processes.
 */
public class ProcessManipulator implements SubjectManipulator {
  private static final Logger log = Logger.getLogger(ProcessManipulator.class.getCanonicalName());

  @Override
  public void restartGroup(SubjectGroup group, long switchRateLimit, long maxWaitMillis) {
    // TODO(drk): implement this. See Process.restartProcessGroup().
  }

  @Override
  public void restartIndividual(Subject individual, long maxWaitMillis) {
    // TODO(drk): implement this. See Process.restartProcess().
  }

  @Override
  public int getPopulationSize(SubjectGroup group, long maxWaitMillis) {
    int populationSize = 0;
    // TODO(drk): support named process groups
    try {
      int processGroupId = Integer.parseInt(group.getName());
      List<Integer> processIds = Process.getProcessIdsOf(processGroupId);
      populationSize = processIds.size();
    } catch (NumberFormatException e) {
      log.log(Level.WARNING, "ProcessManipulator doesn't support named process groups.");
    }
    return populationSize;
  }
}