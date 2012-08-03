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


import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.subject.HealthQuerier;
import org.arbeitspferde.groningen.subject.Subject;

@PipelineIterationScoped
public class NullHealthQuerier implements HealthQuerier {

  @Override
  public boolean blockUntilHealthy(Subject subject) {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public boolean isHealthy(Subject subject) {
    throw new RuntimeException("Not implemented.");
  }
}
