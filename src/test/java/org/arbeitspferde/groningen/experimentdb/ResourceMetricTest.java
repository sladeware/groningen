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

import org.arbeitspferde.groningen.experimentdb.ResourceMetric;

/**
 * The test for {@link ResourceMetric}.
 */
public class ResourceMetricTest extends TestCase {

  private static final long   TEST_MEM_VALUE_A    = 128;

  /** The object we are testing */
  private ResourceMetric r;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    r = new ResourceMetric();
    r.setMemoryFootprint(TEST_MEM_VALUE_A);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testSanityOfResourceMetric() {
    assertEquals(1.0 / TEST_MEM_VALUE_A, r.computeScore(ResourceMetric.ScoreType.MEMORY));
  }

  public void testInvalidate() {
    r.invalidate();
    assertEquals(0.0, r.computeScore(ResourceMetric.ScoreType.MEMORY));
  }
}
