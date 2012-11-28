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

package org.arbeitspferde.groningen.common;


import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;

public class EvaluatedSubjectTest extends ClockedExperimentDbTestCaseBase {
  private SubjectStateBridge subject;
  private double fitness;
  private EvaluatedSubject evaluatedSubject;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    subject = experimentDb.subjects.make();

    final JvmFlagSet.Builder builder = JvmFlagSet.builder()
        .withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 1)
        .withValue(JvmFlag.CMS_EXP_AVG_FACTOR, 3)
        .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, 4)
        .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, 5)
        .withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, 6)
        .withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, 7)
        .withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, 8)
        .withValue(JvmFlag.GC_TIME_RATIO, 9)
        .withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, 10)
        .withValue(JvmFlag.MAX_HEAP_FREE_RATIO, 11)
        .withValue(JvmFlag.MIN_HEAP_FREE_RATIO, 12)
        .withValue(JvmFlag.NEW_RATIO, 13)
        .withValue(JvmFlag.MAX_NEW_SIZE, 14)
        .withValue(JvmFlag.PARALLEL_GC_THREADS, 15)
        .withValue(JvmFlag.SURVIVOR_RATIO, 16)
        .withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, 17)
        .withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, 18)
        .withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, 19)
        .withValue(JvmFlag.CMS_INCREMENTAL_MODE, 1)
        .withValue(JvmFlag.CMS_INCREMENTAL_PACING, 0)
        .withValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY, 1)
        .withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, 0)
        .withValue(JvmFlag.USE_PARALLEL_GC, 1)
        .withValue(JvmFlag.USE_PARALLEL_OLD_GC, 0)
        .withValue(JvmFlag.USE_SERIAL_GC, 0);

    final JvmFlagSet jvmFlagSet = builder.build();
    subject.storeCommandLine(jvmFlagSet);

    fitness = 21.17;
    evaluatedSubject = new EvaluatedSubject(clock, subject, fitness);
  }

  /** Test set and get methods */
  public void testGetSubject() {
    assertEquals(evaluatedSubject.getBridge(), subject);
  }

  public void testGetFitness() {
    assertEquals(evaluatedSubject.getFitness(), fitness);
  }

  public void testGetExperimentId() {
    try {
      evaluatedSubject.getExperimentId();
      fail("Expected EvaluatedSubjectException");
    } catch (final IllegalStateException expectedSoIgnore) { /* ignore */ }
  }

  public void testSetExperimentId() {
    try {
      evaluatedSubject.setExperimentId(-1);
      fail("Expected EvaluatedSubjectException");
    } catch (final IllegalArgumentException expectedSoIgnore) { /* ignore */ }

    evaluatedSubject.setExperimentId(2);
    assertEquals(2, evaluatedSubject.getExperimentId());
  }

  /** Tests the Comparable interface */
  public void testCompareTo() {
    assertTrue(evaluatedSubject.compareTo(new EvaluatedSubject(clock, subject, fitness)) == 0);
    assertTrue(evaluatedSubject.compareTo(new EvaluatedSubject(clock, subject, fitness + 1)) < 0);
    assertTrue(evaluatedSubject.compareTo(new EvaluatedSubject(clock, subject, fitness - 1)) > 0);
  }
}
