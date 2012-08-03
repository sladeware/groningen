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
import com.google.inject.Guice;
import com.google.inject.Module;

/**
 * {@link WorkhorseFactory} is designed to furnish {@link GroningenWorkhorse} instances based off
 * the provided Guice {@link Module}.  {@link OpenMain} provides a canonical example of how easy
 * it would be for another user of Groningen to extend the test bed for new purposes.
 */
public class WorkhorseFactory {
  public GroningenWorkhorse workhorseFor(final Module... modules) {
    return Guice.createInjector(modules).getInstance(GroningenWorkhorse.class);
  }
}
