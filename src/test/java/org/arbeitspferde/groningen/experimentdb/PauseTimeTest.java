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


import junit.framework.TestCase;

import org.arbeitspferde.groningen.experimentdb.PauseTime;

/**
 * The test for {@link PauseTimes}.
 */
public class PauseTimeTest extends TestCase {
  private static final long TEST_GC_RECORD_ID_A = 1234;
  private static final double TEST_PAUSE_VALUE_A = 2.0;

  /** The object we are testing */
  private PauseTime p;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    p = new PauseTime();
    p.incrementPauseTime(TEST_PAUSE_VALUE_A);
  }

  public void testSanityOfPauseTime() {
    assertEquals(TEST_PAUSE_VALUE_A, p.getPauseTimeTotal());

    p.incrementPauseTime(TEST_PAUSE_VALUE_A);
    assertEquals(1.0 / (2.0 * TEST_PAUSE_VALUE_A), p.computeScore(PauseTime.ScoreType.THROUGHPUT));
    assertEquals(1.0 / TEST_PAUSE_VALUE_A, p.computeScore(PauseTime.ScoreType.LATENCY));
  }

  public void testInvalidate() {
    p.invalidate();
    assertEquals(0.0, p.computeScore(PauseTime.ScoreType.THROUGHPUT));
    assertEquals(0.0, p.computeScore(PauseTime.ScoreType.LATENCY));
  }

  public void testSetPercentile() {
    assertEquals(99.0, p.percentile);
    p.setPercentile(66);
    assertEquals(66.0, p.percentile);
    PauseTime n = new PauseTime(17.45);
    assertEquals(17.45, n.percentile);
  }

}
