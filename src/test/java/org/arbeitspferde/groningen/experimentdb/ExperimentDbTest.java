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


import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCase;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;

import java.util.Arrays;

/**
 * The test for {@link ExperimentDb}.
 */
public class ExperimentDbTest extends ClockedExperimentDbTestCase {
  private static final int TEST_EXPERIMENT_DURATION = 3;
  private static final int TEST_RESTART_THRESHOLD = 7;

  public void testArguments() throws Exception {
    experimentDb.putArguments(TEST_EXPERIMENT_DURATION,
        TEST_RESTART_THRESHOLD);
    assertEquals(experimentDb.getExperimentDuration(),
        TEST_EXPERIMENT_DURATION);
    assertEquals(experimentDb.getRestartThreshold(), TEST_RESTART_THRESHOLD);
  }

  public void testExperimentCache() throws Exception {
    Experiment e1 = experimentDb.getExperiments().make(Arrays.asList(2L));

    // Verify the results.
    Experiment cachedExperiment = experimentDb.getExperiments().lookup(e1.getIdOfObject());
    assertSame(e1, cachedExperiment);
  }

  /**
   * Tests {@link Experiment#lookup(long)} method with invalid arguments.
   */
  public void testExperimentLookupWithInvalidArguments() throws Exception {
    try {
      experimentDb.getExperiments().lookup(-1);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) { /* ignore */ }
  }

  /**
   * Tests {@link Experiment#lookup(long)} method with an empty cache.
   */
  public void testExperimentLookupWithEmptyCache() throws Exception {
    Experiment experiment = experimentDb.getExperiments().lookup(1L);
    assertNull(experiment);
  }

  /**
   * Tests {@link Experiment#getLastCachedExperimentId()} method.
   */
  public void testGetLastCachedExperiment() throws Exception {
    // Cache a couple experiments
    Experiment experiment1 = experimentDb.getExperiments().make(Arrays.asList(2L));
    Experiment experiment2 = experimentDb.getExperiments().make(Arrays.asList(3L));

    assertTrue(experiment1.getIdOfObject() != experiment2.getIdOfObject());
    // Make sure you get back the last experiment you cached
    assertSame(experiment2, experimentDb.getExperiments().getLast());
  }

  public void testSubjectCache() throws Exception {
    SubjectStateBridge e1 = experimentDb.subjects.make();

    // Verify the results.
    SubjectStateBridge cachedSubject = experimentDb.subjects.lookup(e1.getIdOfObject());
    assertSame(e1, cachedSubject);
  }

  /**
   * Tests {@link SubjectStateBridge#lookup(long)} method with invalid arguments.
   */
  public void testSubjectLookupWithInvalidArguments() throws Exception {
    try {
      experimentDb.subjects.lookup(-1);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) { /* ignore */ }
  }

  /**
   * Tests {@link Experiment#lookup(long)} method with an empty cache.
   */
  public void testSubjectLookupWithEmptyCache() throws Exception {
    SubjectStateBridge subject = experimentDb.subjects.lookup(1L);
    assertNull(subject);
  }

  public void testGetExperimentId() {
    assertEquals(0, experimentDb.getExperimentId());
  }

  public void testGetExperimentId_postExperiment() {
    experimentDb.nextExperimentId();

    assertEquals(1, experimentDb.getExperimentId());
  }
}
