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
import com.google.inject.Injector;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.BaseModule;
import org.arbeitspferde.groningen.GroningenConfigParamsModule;
import org.arbeitspferde.groningen.ServicesModule;
import org.arbeitspferde.groningen.eventlog.EventLoggerService;
import org.arbeitspferde.groningen.open.OpenModule;

/**
 * Tests for {@link ServicesModule}.
 */
public class ServicesModuleTest extends TestCase {
  private Injector injector;

  @Override
  protected void setUp() throws Exception {
    // Use the testing infrastructure's temporary directory management infrastructure.
    final String[] args = new String[] {};

    injector = Guice.createInjector(
        new BaseModule(args), new GroningenConfigParamsModule(),
        new ServicesModule(), new OpenModule());
  }

  public void testInjectorProvision() {
    assertNotNull(injector);
  }

  // This is merely here as a sanity check against cross-Guice module interactions.
  public void testInjectorProvision_eventLoggerService() {
    final EventLoggerService eventLoggerService = injector.getInstance(EventLoggerService.class);

    assertNotNull(eventLoggerService);
  }

//  public void testInjectorProvision_eventLogger() {
//    final EventLoggerService eventLoggerService = injector.getInstance(EventLoggerService.class);
//
//    /* We could conceivably use AbstractIdleService#startAndWait, but I'd rather just make the test
//     * as fast as possible.
//     *
//     * The service needs to be started before the logger can be dispatched.  :-)
//     */
//    final ListenableFuture<State> result = eventLoggerService.start();
//
//    try {
//      // Wait at most one minute for result.
//      final State state = result.get(1, TimeUnit.MINUTES);
//    } catch (final Exception e) {
//      fail("Could not get Future<State>: " + e);
//    }
//
//    // TypeLiteral is used to request named generics from the injector.
//    final TypeLiteral<SafeProtoLogger<Event.EventEntry>> keyLiteral =
//        new TypeLiteral<SafeProtoLogger<Event.EventEntry>>(){};
//    final SafeProtoLogger<Event.EventEntry> eventLogger =
//        injector.getInstance(Key.get(keyLiteral, Names.named("eventLogger")));
//
//    assertNotNull(eventLogger);
//  }
}
