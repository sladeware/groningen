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

package org.arbeitspferde.groningen;


import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

/**
 * Test for {@link PipelineId}
 */
public class PipelineIdTest extends TestCase {
  public void testEqualityIsConsistent() {
    PipelineId id1 = new PipelineId("id1");
    PipelineId id1Copy = new PipelineId("id1");
    PipelineId id2 = new PipelineId("id2");

    new EqualsTester()
        .addEqualityGroup(id1, id1, id1Copy)
        .addEqualityGroup(id2)
        .testEquals();
  }
}

