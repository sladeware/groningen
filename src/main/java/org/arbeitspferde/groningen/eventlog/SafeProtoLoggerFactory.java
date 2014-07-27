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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory.Specification;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * A producer of {@link SafeProtoLogger}.
 *
 * This class is rather trivial, but it exists to keep the complexity of {@link SafeProtoLogger}'s
 * constructor to a minimum.
 */
@Singleton
public class SafeProtoLoggerFactory {
  private static final Logger log = Logger.getLogger(SafeProtoLogger.class.getCanonicalName());

  private final Provider<Timer> timerProvider;
  private final OutputLogStreamFactory outputLogStreamFactory;

  /**
   * Create factory that produces safe protocol buffer loggers.
   *
   * @param timerProvider The {@link Provider} or {@link Timer} for testability.
   */
  @Inject
  SafeProtoLoggerFactory(final Provider<Timer> timerProvider,
      final OutputLogStreamFactory outputLogStreamFactory) {
    this.timerProvider = timerProvider;
    this.outputLogStreamFactory = outputLogStreamFactory;
  }

  /**
   * Create a new {@link SafeProtoLogger} of {@link Event.EventEntry}.
   *
   * @param basename See {@link Specification#getFilenamePrefix()}.
   * @param port See {@link Specification#getServingPort()}.
   * @param rotateSize See {@link Specification#rotateFileUponAtSizeBytes()}.
   * @param flushIntervalSeconds The interval in seconds between automatic log flush events.
   * @param loggerName The human-readable canonical name for the log.
   *
   * @return A {@link SafeProtoLogger} that meets the supra specifications.
   *
   * @throws IOException In the event that the RecordIO cannot be provisioned.
   */
  public SafeProtoLogger<Event.EventEntry> newEventEntryLogger(final String basename,
      final int port, final long rotateSize, final int flushIntervalSeconds,
      final String loggerName) throws IOException {
    Preconditions.checkNotNull(basename, "basename may not be null.");
    Preconditions.checkArgument(port > 0, "port must be > 0.");
    Preconditions.checkArgument(rotateSize >= 0, "rotateSize must >= 0.");
    Preconditions.checkArgument(flushIntervalSeconds >= 0, "flushIntervalSeconds must >= 0.");
    Preconditions.checkNotNull(loggerName, "loggerName may not be null.");

    log.info(String.format("Creating RecordIO log around %s.", basename));


    final OutputLogStream stream = outputLogStreamFactory.rotatingStreamForSpecification(
        new Specification() {

          @Override
          public String getFilenamePrefix() {
            return basename;
          }

          @Override
          public int getServingPort() {
            return port;
          }

          @Override
          public long rotateFileUponAtSizeBytes() {
            return rotateSize;
          }
        });

    final SafeProtoLogger<Event.EventEntry> logger =
        new SafeProtoLogger<>(stream, loggerName);

    yieldTimer(flushIntervalSeconds, loggerName, logger);

    return logger;
  }

  /**
   * Create a timer that automatically flushes the RecordIO stream.
   *
   * No hard reference needs to be held onto the {@link Timer} per its Javadoc.
   *
   * @param flushIntervalSeconds See {@link #newEventEntryLogger(String, int, long, int, String)}.
   * @param loggerName See {@link #newEventEntryLogger(String, int, long, int, String)}.
   * @param logger A {@link SafeProtoLogger}.
   *
   * @return The {@link Timer} that manages the automatic flushing.
   */
  @VisibleForTesting
  Timer yieldTimer(final int flushIntervalSeconds, final String loggerName,
      final SafeProtoLogger<?> logger) {
    Preconditions.checkNotNull(loggerName, "loggerName may not be null.");
    Preconditions.checkNotNull(logger, "stream may not be null.");

    final Timer flushEventTimer = timerProvider.get();

    if (flushIntervalSeconds > 0) {
      flushEventTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          try {
              log.info(String.format("Flushing %s...", loggerName));
              logger.flush();
              log.info(String.format("Finished flushing %s.", loggerName));
          } catch (final IOException e) {
            log.warning(String.format("Failed to flush log for %s.", loggerName, e));
          }
        }
      }, 0, flushIntervalSeconds * 1000L);
    }

    return flushEventTimer;
  }
}
