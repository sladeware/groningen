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

package org.arbeitspferde.groningen.config;
/**
 * Provide a means of forward-porting legacy configurations into whatever is used du jour.
 */
public interface LegacyProgramConfigurationMediator {
  // TODO(team): Make this API more idiomatic for the Java Standard Library as appropriate.

  /**
   * @param legacyConfiguration The old configuration verbatim.
   * @return A presently-compatible version of the configuration.
   */
  public String migrateLegacyConfiguration(final String legacyConfiguration);
}
