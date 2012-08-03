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

import org.arbeitspferde.groningen.experimentdb.BaseComputeScore;

public class ComputeScoreTest extends TestCase {

  enum TestEnum { A, B };

  public void testInvalidScores() throws Exception {
    BaseComputeScore testScore = new BaseComputeScore() {
        @Override
        protected double computeScoreImpl(Enum scoreType) {
          return scoreType.name().charAt(0);
        }
      };
    assertEquals(65.0, testScore.computeScore(TestEnum.A));
    assertEquals(66.0, testScore.computeScore(TestEnum.B));
    testScore.invalidate();
    assertEquals(0.0, testScore.computeScore(TestEnum.A));
    assertEquals(0.0, testScore.computeScore(TestEnum.B));
  }
}
