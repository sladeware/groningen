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


import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * The test for {@link Statistics}.
 */
public class StatisticsTest extends TestCase {

  public void testComputePecentile() throws Exception {
    ArrayList<Double> values = makeIncrementalList(313.0);
    assertEquals(Statistics.computePercentile(values, 99), 309.0);
    assertEquals(Statistics.computePercentile(values, 76), 237.0);
    assertEquals(Statistics.computePercentile(values, 100), 312.0);
    assertEquals(Statistics.computePercentile(values, 0), 0.0);

    values = makeIncrementalList(312.0);
    assertEquals(Statistics.computePercentile(values, 50), 156.0);
    assertEquals(Statistics.computePercentile(values, 12.52), 39.0);
    assertEquals(Statistics.computePercentile(values, 99.999), 311.0);
  }

  public void testInvalidPecentile() throws Exception {
    ArrayList<Double> values = makeIncrementalList(313.0);

    try {
      Statistics.computePercentile(values, 101);
      fail("Should have thrown exception, percentile being 101");
    } catch (IllegalArgumentException expected) {
      // expected failure b/c percentile argument wrong
    }

    try {
      Statistics.computePercentile(values, -1);
      fail("Should have thrown exception, percentile being -1");
    } catch (IllegalArgumentException expected) {
      // expected failure b/c percentile argument wrong
    }
}

  /** Returns a 0.0 to ceil incremental ArrayList */
  private ArrayList<Double> makeIncrementalList(double ceil) {
    ArrayList<Double> values = new ArrayList<Double>();
    for (double i = 0.0; i < ceil; i += 1.0) {
      values.add(i);
    }
    return values;
  }
}
