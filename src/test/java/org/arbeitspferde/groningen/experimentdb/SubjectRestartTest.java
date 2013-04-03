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
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.easymock.EasyMock;

/**
 * The test for {@link SubjectRestart}.
 */
public class SubjectRestartTest extends ClockedExperimentDbTestCaseBase {
  private static final int TEST_RESTART_THRESHOLD = 2;

  /** The object we are testing */
  private SubjectRestart t;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    t = new SubjectRestart();
    t.anotherRestart();
    t.anotherRestart();
  }

  public void testSanity() {
    assertEquals(2, t.getNumberOfRestarts());
  }

  public void testRestartThresholdCrossed() {
    GroningenConfig mockGroningenConfig = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParamsOrBuilder mockParams = EasyMock.createNiceMock(GroningenParamsOrBuilder.class);
    EasyMock.expect(mockGroningenConfig.getParamBlock()).andReturn(mockParams).anyTimes();
    EasyMock.expect(mockParams.getRestart()).andReturn(TEST_RESTART_THRESHOLD).anyTimes();
    EasyMock.replay(mockGroningenConfig, mockParams);
    
    SubjectRestart t2 = new SubjectRestart();
    t2.anotherRestart();

    assertTrue(t.restartThresholdCrossed(mockGroningenConfig));
    assertFalse(t2.restartThresholdCrossed(mockGroningenConfig));
  }
}
