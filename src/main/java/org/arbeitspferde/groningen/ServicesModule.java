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

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.eventlog.EventLoggerService;
import org.arbeitspferde.groningen.eventlog.SafeProtoLogger;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.utility.MetricsService;

/**
 * A Guice module for Groningen's start-up and shutdown services.
 *
 * Unlike {@link BaseModule}, this Guice module uses {@link Multibinder}, which enables one to
 * create injectable bundles of objects for at-once delivery.  In our case, this uses {@link Set}
 * to house such things.
 *
 * The services are considered to be at-once injections due to the fact that they are needed
 * altogether versus on-demand.  They use {@link AbstractIdleService}'s facilities for
 * dispatching, etc.
 */
public class ServicesModule extends AbstractModule {
  @Override
  protected void configure() {
    final Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);

    serviceBinder.addBinding().to(EventLoggerService.class);
    serviceBinder.addBinding().to(MetricsService.class);
  }

  @Provides
  @Singleton
  @Named("eventLogger")
  public SafeProtoLogger<Event.EventEntry> provideEventLogger(
      final EventLoggerService eventLoggerService) {
    return eventLoggerService.getLogger();
  }
}
