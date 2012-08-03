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

package org.arbeitspferde.groningen.subject.open;

import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.subject.SubjectManipulator;

public class NullSubjectManipulator implements SubjectManipulator {
  @Override
  public void restartGroup(SubjectGroup group, long switchRateLimit, long maxWaitMillis) {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public void restartIndividual(Subject individual, long maxWaitMillis) {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public int getPopulationSize(SubjectGroup group, long maxWaitMillis) {
    throw new RuntimeException("Not implemented.");
  }
}
