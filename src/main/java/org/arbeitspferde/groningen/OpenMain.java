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
import org.arbeitspferde.groningen.open.OpenModule;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link OpenMain} is the primary entrypoint into the open source edition of Groningen.
 *
 * Note its simplicity: It is designed to be a template for future extension and enhancement as
 * provided by the system of Guice bindings via {@link WorkhorseFactory}.
 */
public class OpenMain {
  private static final Logger log = Logger.getLogger(OpenMain.class.getCanonicalName());

  public static void main(final String[] args) {
    try {
      final WorkhorseFactory workhorseFactory = new WorkhorseFactory();
      final GroningenWorkhorse groningenWorkhorse = workhorseFactory.workhorseFor(
          new OpenModule(),
          new BaseModule(args),
          new ServicesModule(),
          new GroningenConfigParamsModule());

      groningenWorkhorse.run();
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Uncaught exception; aborting.", e);
    }
  }
}
