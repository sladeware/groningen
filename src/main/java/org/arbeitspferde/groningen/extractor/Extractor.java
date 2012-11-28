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

package org.arbeitspferde.groningen.extractor;

import com.google.common.base.Optional;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.utility.FileFactory;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of {@code Extractor} is given a {@link SubjectStateBridge} in its constructor
 * to operate on. The Extractor uses it to retrieve a log file from a subject and
 * then parses it for signal data used by the Hypothesizer. This is thread safe.
 */
public class Extractor implements Runnable {
  private static final String PAUSE_TIME_LINE_SIGNATURE =
      "Total time for which application threads were stopped:";

  /** The depth of tokens we examine when parsing */
  private static final int TOKEN_DEPTH = 9;

  private static final Logger log = Logger.getLogger(Extractor.class.getCanonicalName());
  private static final AtomicBoolean hasRegistered = new AtomicBoolean(false);
  private static final AtomicLong logSuccessfulParseCount = new AtomicLong();
  private static final AtomicLong logFailedParseCount = new AtomicLong();

  private Optional<GroningenConfig> config;
  private SubjectStateBridge bridge;
  private FileFactory fileFactory;
  private Optional<CollectionLogAddressor> addressor;

  /** When set to true, we print out parsing details */
  private Boolean verbose;

  public Extractor(final GroningenConfig config, final SubjectStateBridge bridge,
      final MetricExporter metricExporter, final FileFactory fileFactory,
      final CollectionLogAddressor collectionLogAddressor) {
    initialize(config, bridge, true, metricExporter, fileFactory,
        collectionLogAddressor);
  }

  public Extractor(final SubjectStateBridge bridge, final Boolean verbose,
      final FileFactory fileFactory) {
    initialize(null, bridge, verbose, null, fileFactory, null);
  }

  private void initialize(@Nullable final GroningenConfig config, final SubjectStateBridge bridge,
      final Boolean verbose, @Nullable final MetricExporter exporter,
      final FileFactory fileFactory,
      @Nullable final CollectionLogAddressor collectionLogAddressor) {
    this.config = Optional.fromNullable(config);
    this.bridge = bridge;
    this.verbose = verbose;
    this.fileFactory = fileFactory;
    this.addressor = Optional.fromNullable(collectionLogAddressor);

    final Optional<MetricExporter> metricExporter = Optional.fromNullable(exporter);

    if (metricExporter.isPresent() && !hasRegistered.get()) {
      hasRegistered.set(true);
      final MetricExporter agent = metricExporter.get();

      agent.register(
          "extractor_successful_log_parses_total",
          "The total number of log files sucessfully parsed.",
          Metric.make(logSuccessfulParseCount));
      agent.register(
          "extractor_failed_log_parses_total",
          "The total number of log files that we have failed to parse.",
          Metric.make(logFailedParseCount));
    }
  }

  @Override
  public void run() {
    if (!config.isPresent()) {
      log.severe("Extractor not processing due to null config");
      return;
    }

    final Subject subject = bridge.getAssociatedSubject();

    if (verbose) {
      log.info(String.format("Extractor processing subject id: %s", bridge.getIdOfObject()));
    }

    final String logLocation = addressor.get().logPathFor(subject, config.get());

    parse(logLocation);
  }

  /** Parse the input log filename and update signals as required */
  public void parse(String filename) {
    try {

      final InputStream inputStream =
          fileFactory.forFile(filename).inputStreamFor();
      final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

      String line = null;
      String token = null;
      boolean parserActivated = true;
      while ((line = bufferedReader.readLine()) != null) {
        // We use -XX:+PrintGCApplicationStoppedTime to output pause time data that we parse here
        // from the input gc.log or STDOUT of the experimental subject
        int beginIndex = line.indexOf(PAUSE_TIME_LINE_SIGNATURE);
        if (beginIndex >= 0) {
          // Filter output.
          if (parserActivated) {
            parserActivated = false;

            final StringTokenizer stringTokenizer = new StringTokenizer(line.substring(beginIndex));

            for (int i = 0; i < TOKEN_DEPTH; i++) {
              if (stringTokenizer.hasMoreTokens()) {
                token = stringTokenizer.nextToken();
              } else {
                break;
              }
            }

            double pauseTimeSecs;

            try {
              pauseTimeSecs = Double.valueOf(token.trim()).doubleValue();
            } catch (final Exception e) {
              log.log(Level.WARNING,
                  String.format("Unable to parse pause time '%s'. Defaulting to 0.0", token), e);
              pauseTimeSecs = 0.0;
            }
            bridge.getPauseTime().incrementPauseTime(pauseTimeSecs);
            if (verbose) {
              log.info(String.format("Paused %s seconds", token));
            }
          }
        } else {
          parserActivated = true;
        }
      }
      bufferedReader.close();
      logSuccessfulParseCount.incrementAndGet();
    } catch (final IOException e) {
      log.log(Level.WARNING, "Problems processing the log file.", e);
      logFailedParseCount.incrementAndGet();
    }
  }
}
