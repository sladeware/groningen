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


import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;

import java.util.Arrays;

/**
 * The test for {@link ExperimentDb}.
 */
public class ExperimentDbTest extends ClockedExperimentDbTestCaseBase {
  public void testLastExperiment() throws Exception {
    Experiment e1 = experimentDb.makeExperiment(Arrays.asList(2L));

    // Verify the results.
    Experiment cachedExperiment = experimentDb.getLastExperiment();
    assertSame(e1, cachedExperiment);
  }

  public void testExperimentLookupWithEmptyCache() throws Exception {
    Experiment experiment = experimentDb.getLastExperiment();
    assertNull(experiment);
  }

  /**
   * Tests {@link Experiment#getLastCachedExperimentId()} method.
   */
  public void testGetLastCachedExperiment() throws Exception {
    // Cache a couple experiments
    Experiment experiment1 = experimentDb.makeExperiment(Arrays.asList(2L));
    Experiment experiment2 = experimentDb.makeExperiment(Arrays.asList(3L));

    assertTrue(experiment1.getIdOfObject() != experiment2.getIdOfObject());
    // Make sure you get back the last experiment you cached
    assertSame(experiment2, experimentDb.getLastExperiment());
  }

  public void testSubjectCache() throws Exception {
    SubjectStateBridge e1 = experimentDb.makeSubject();

    // Verify the results.
    SubjectStateBridge cachedSubject = experimentDb.lookupSubject(e1.getIdOfObject());
    assertSame(e1, cachedSubject);
  }

  /**
   * Tests {@link SubjectStateBridge#lookup(long)} method with invalid arguments.
   */
  public void testSubjectLookupWithInvalidArguments() throws Exception {
    try {
      experimentDb.lookupSubject(-1);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expectedSoIgnore) { /* ignore */ }
  }

  /**
   * Tests {@link Experiment#lookup(long)} method with an empty cache.
   */
  public void testSubjectLookupWithEmptyCache() throws Exception {
    SubjectStateBridge subject = experimentDb.lookupSubject(1L);
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
