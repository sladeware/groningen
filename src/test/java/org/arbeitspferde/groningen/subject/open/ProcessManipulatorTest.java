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

import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.utility.Process;

import junit.framework.TestCase;

/**
 * Test for {@link ProcessManipulator}.
 */
public class ProcessManipulatorTest extends TestCase {

  public SubjectGroup genSubjectGroup(String groupName) {
    SubjectGroup subjectGroup = new SubjectGroup("home_cluster", groupName, "me",
        new StubConfigManager.StubGroupConfig(), new ProcessServingAddressGenerator());
    return subjectGroup;
  }

  public void testPopulationSizeOfUnexistedProcess() {
    SubjectGroup subjectGroup = genSubjectGroup("-1");
    ProcessManipulator manipulator = new ProcessManipulator();
    assertEquals(0 , manipulator.getPopulationSize(subjectGroup, -1));
  }

  public void testPopulationSizeOfThisProcess() {
    SubjectGroup subjectGroup = genSubjectGroup(Integer.toString(Process.myProcessGroupId()));
    ProcessManipulator manipulator = new ProcessManipulator();
    // There should be at least one process in the given subject group
    assertTrue(manipulator.getPopulationSize(subjectGroup, -1) > 0);
  }
}