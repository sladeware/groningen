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

package org.arbeitspferde.groningen.eventlog;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.common.Settings;
import org.arbeitspferde.groningen.proto.Event;

import java.util.logging.Logger;

/**
 * A start-up and shutdown service that provisions and reclaims {@link SafeProtoLogger}s for generic
 * Groningen events.
 *
 * @see {@link AbstractIdleService}
 * @see {@link SafeProtoLogger}
 */
@Singleton
public class EventLoggerService extends AbstractIdleService {
  private static final Logger log = Logger.getLogger(EventLoggerService.class.getCanonicalName());

  private final SafeProtoLoggerFactory loggerFactory;
  private final String eventLogPrefix;
  private final int port;
  private final int eventLogRotateSizeBytes;
  private final int eventLogFlushIntervalSeconds;

  private SafeProtoLogger<Event.EventEntry> logger;

  /**
   * Create a new idle background service.
   *
   * @param loggerFactory The {@link SafeProtoLoggerFactory} that shall be responsible for creating
   *     the loggers.
   * @param settings The {@link Settings} responsible for the runtime mode of operation.
   */
  @Inject
  public EventLoggerService(
      final SafeProtoLoggerFactory loggerFactory,
      final Settings settings) {
    Preconditions.checkNotNull(loggerFactory, "loggerFactory may not be null.");
    Preconditions.checkArgument(
        settings.getEventLogRotateSizeBytes() >= 0, "eventLogRotateSizeBytes must be >= 0.");
    Preconditions.checkArgument(
        settings.getEventLogFlushIntervalSeconds() >= 0, "eventLogFlushInterval must be >= 0.");

    this.loggerFactory = loggerFactory;
    this.eventLogPrefix = settings.getEventLogPrefix();
    this.port = settings.getPort();
    this.eventLogRotateSizeBytes = settings.getEventLogRotateSizeBytes();
    this.eventLogFlushIntervalSeconds = settings.getEventLogFlushIntervalSeconds();
  }

  /**
   * Emit the requisitioned {@link SafeProtoLogger}.
   *
   * @return The logger.
   */
  public SafeProtoLogger<Event.EventEntry> getLogger() {
    // Poor man's state machine.
    Preconditions.checkState(logger != null, "logger has not been initialized.");

    return logger;
  }

  @Override
  protected void startUp() throws Exception {
    log.info("Provisioning RecordIO logger...");
    logger = loggerFactory.newEventEntryLogger(eventLogPrefix, port, eventLogRotateSizeBytes,
        eventLogFlushIntervalSeconds, "genericEventLogger");
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("Closing down RecordIO logger...");

    if (logger != null) {
      logger.flush();
    }
  }
}
