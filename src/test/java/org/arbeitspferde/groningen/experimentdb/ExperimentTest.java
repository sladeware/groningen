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

package org.arbeitspferde.groningen.experimentdb;

import com.google.common.collect.ImmutableList;

import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCase;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test class for {@link Experiment}.
 */
public class ExperimentTest extends ClockedExperimentDbTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Tests constructor method with invalid arguments.
   */
  public void testCreationWithInvalidArguments() throws Exception {
    try {
      new Experiment(null, 1L, Arrays.asList(1L));
      fail("Expected exception.");
    } catch (NullPointerException expectedSoIgnore) {}

    try {
      new Experiment(experimentDb, 0L, Arrays.asList(1L));
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) {}

    try {
      new Experiment(experimentDb, 1L, null);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) {}

    try {
      new Experiment(experimentDb, 1L, Collections.<Long>emptyList());
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) {}
  }

  /**
   * Tests constructor.
   */
  public void testCreation() throws Exception {
    Experiment e = new Experiment(experimentDb, 1L, Arrays.asList(2L));

    assertEquals(1L, e.getIdOfObject());
    List<Long> subjectIds = e.getSubjectIds();
    assertNotNull(subjectIds);
    assertEquals(1, subjectIds.size());
    assertEquals(2L, (long) subjectIds.get(0));
    // Verify that the Subject IDs list is immutable.
    try {
      subjectIds.add(3L);
      fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException expectedSoIgnore) {}
  }

  /** Tests {@link Experiment.getSubjects} */
  public void testGetSubjects() throws Exception {
    SubjectStateBridge s1 = experimentDb.subjects.make();
    SubjectStateBridge s2 = experimentDb.subjects.make();
    Experiment e = experimentDb.getExperiments().make(ImmutableList.of(s1.getIdOfObject(),
                                                                  s2.getIdOfObject()));
    List<SubjectStateBridge> subjects = e.getSubjects();
    assertEquals(2, subjects.size());
    assertSame(s1, subjects.get(0));
    assertSame(s2, subjects.get(1));
  }

}
