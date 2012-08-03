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
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.subject.ServingAddressGenerator;
import org.arbeitspferde.groningen.subject.SubjectGroup;

import java.util.logging.Logger;

/**
 * A non-functioning generator to build fully-qualified serving address for a cluster
 * environment.  Please implement this per your own requirements.
 */
@Singleton
public class NullServingAddressGenerator implements ServingAddressGenerator {
  private static final Logger log =
      Logger.getLogger(NullServingAddressGenerator.class.getCanonicalName());

  @Override
  public String addressFor(final SubjectGroup group, final int index) {
    log.severe(String.format("Cannot create address for %s index %s.", group, index));

    return String.format("%s @ %s", group, index);
  }
}
