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

package org.arbeitspferde.groningen.generator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.common.SubjectSettingsFileManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.generator.SubjectShuffler.SubjectIterator;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.proto.ExpArgFile.ExperimentArgs;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Generator queries the {@link ExperimentDb} to build an experiment from the output of
 * the {@link Hypothesizer}. The results are written to settings files so that restarting
 * subjects can read the mutated JVM settings.
 */
@PipelineIterationScoped
public class Generator extends ProfilingRunnable {
  /** Logger for this class */
  private static final Logger log = Logger.getLogger(Generator.class.getCanonicalName());

  /** The Expermental Database */
  private final ExperimentDb experimentDb;

  /** The serving address of this Groningen instance */
  private final String servingAddress;

  private final SubjectShuffler subjectShuffler;

  private final SubjectSettingsFileManager subjectSettingsFileManager;

  private final MetricExporter metricExporter;

  /** Counts the number of times the generator failed to update the subject file */
  private static final AtomicLong generatorFailures = new AtomicLong(0);

  private final PipelineId pipelineId;

  /**
   * Reset the JVM settings for a given subject to default values.
   * This method is called by Executor.
   * @param subject The subject whose subject to be reset.
   */
  public void resetToDefault(SubjectStateBridge subject) {
    subjectSettingsFileManager.delete(subject.getAssociatedSubject().getExpSettingsFile());
  }

  @Inject
  public Generator(PipelineId pipelineId,
      final Clock clock, final MonitorGroningen monitor, final ExperimentDb e,
      @Named("servingAddress") final String servingAddress, final SubjectShuffler subjectShuffler,
      final SubjectSettingsFileManager subjectSettingsFileManager,
      final MetricExporter metricExporter) {
    super(clock, monitor);

    experimentDb = e;
    this.servingAddress = servingAddress;
    this.subjectShuffler = subjectShuffler;
    this.subjectSettingsFileManager = subjectSettingsFileManager;
    this.metricExporter = metricExporter;
    this.pipelineId = pipelineId;
  }

  @Override
  public void profiledRun(final GroningenConfig config) throws RuntimeException {
    final Experiment lastExperiment;
    lastExperiment = experimentDb.getLastExperiment();
    if (lastExperiment == null) {
      log.warning("Experiments do not exist. Skipping Generator stage.");
    } else {
      try {
        final SubjectIterator subjectIterator = subjectShuffler.createIterator();

        for (final SubjectStateBridge bridge : lastExperiment.getSubjects()) {
          if (subjectIterator.hasNext()) {
            Subject subject = subjectIterator.next();
            bridge.setAssociatedSubject(subject);
            // Do not build and write experiment args for default subjects. Delete the settings
            // file, so injector won't be able to read the new settings.
            if (subject.isDefault()) {
              subjectSettingsFileManager.delete(subject.getExpSettingsFile());
              continue;
            }
            final ExperimentArgs experimentArgs = ExperimentArgs.newBuilder()
                .setArgs(bridge.getCommandLine().toArgumentString())
                .setMasterServingAddress(servingAddress)
                .addAllTunedArg(CommandLine.getManagedArgs())
                .setPipelineId(pipelineId.id())
                .build();
            subjectSettingsFileManager.write(experimentArgs, subject.getExpSettingsFile());
          }
        }
      } catch (final Exception e) {
        generatorFailures.incrementAndGet();
        log.log(Level.WARNING, "Unexpected exception when processing subjects.", e);
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void startUp() {
    log.info("Initializing Generator.");

    /*
     *  TODO(team): Take care of making sure that this works and does not leak memory once
     *  multiple concurrent pipelines are supported.
     */

    metricExporter.register(
        "generator_failures",
        "Counts the number of times the generator failed to update the subject file",
        Metric.make(generatorFailures));
  }
}
