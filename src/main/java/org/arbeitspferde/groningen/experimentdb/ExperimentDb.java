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

package org.arbeitspferde.groningen.experimentdb;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.proto.ExperimentDbProtos;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.FileFactory;
import org.arbeitspferde.groningen.utility.InputLogStream;
import org.arbeitspferde.groningen.utility.InputLogStreamFactory;
import org.arbeitspferde.groningen.utility.OutputLogStream;
import org.arbeitspferde.groningen.utility.OutputLogStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ExperimentDb contains Groningen data generated and used by each of the major Groningen
 * components.  The primary purpose of the Experiment Database is to hold global data that is
 * shared between the Groningen pipeline stages. This includes the JVM parameters of mutated
 * subjects, their GC log data and the updated JVM arguments comprising a new population. Pipeline
 * stages do not directly share data, but instead read and write the ExperimentDb.
 */
@PipelineScoped
public class ExperimentDb {
  /*
   * TODO(team): Add all calls for pause time caching to blocking GC phases
   * TODO(team): Store JVM arguments from subjects
   * TODO(team): Implement get methods to feed pipe stages
   * TODO(team): Store mutated JVM arguments for the next generation
   * TODO(team): Complete caching PauseTime and ResourceMetric for scoring
   */

  /** Logger for this class */
  private static final Logger log = Logger.getLogger(ExperimentDb.class.getCanonicalName());

  @Inject
  @NamedConfigParam("checkpoint_file")
  private Provider<String> checkpointFile = new Provider<String>() {
    @Override
    public String get() {
      return GroningenParams.getDefaultInstance().getCheckpointFile();
    }
  };

  private static final Joiner spaceSeparator = Joiner.on(" ");
  private static final Joiner commaSpaceSeparator = Joiner.on(", ");

  /** We use a Clock object for time keeping, which can be easily mocked. */
  private final Clock clock;
  private final FileFactory fileFactory;
  private final InputLogStreamFactory inputLogStreamFactory;
  private final OutputLogStreamFactory outputLogStreamFactory;

  /** A subject id counter */
  private long localSubjectId = 0;

  /**
   * The current ID of experiments.  It is guaranteed to be unique per program invocation and
   * hosting address but nothing more.
   */
  private long currentExperimentId = 0;

  /** current parameters values for running this instance of Groningen. */
  private int experimentDuration = 0;
  private int restartThreshold = 0;

  /** Experiments of this run */
  private final ExperimentCache experiments = new ExperimentCache();

  /** Subjects of this run */
  public final SubjectCache subjects = new SubjectCache();

  @Inject
  public ExperimentDb(final Clock clock, final FileFactory fileFactory,
      final InputLogStreamFactory inputLogStreamFactory,
      final OutputLogStreamFactory outputLogStreamFactory) {
    this.clock = clock;
    this.fileFactory = fileFactory;
    this.inputLogStreamFactory = inputLogStreamFactory;
    this.outputLogStreamFactory = outputLogStreamFactory;
  }

  /** Returns a string serializing and displaying list elements for humans. */
  public static String buildStringFromList(final List<?> list) {
    return commaSpaceSeparator.join(list);
  }

  public static void write(final String methodName, final Object... values) {
    final StringBuilder output = new StringBuilder();
    output.append("ExperimentDb ").append(methodName).append(" :");
    spaceSeparator.appendTo(output, values);
    log.info(output.toString());
  }

  /** The logger is being made available so tests can add a handler */
  public static Logger getLog() {
    return log;
  }

  /**
   * Stores the current values of parameters passed into Groningen. Volatile
   * storage.
   */
  public void putArguments(final int experimentDuration, final int restartThreshold) {
    this.experimentDuration = experimentDuration;
    this.restartThreshold = restartThreshold;

    write("putArguments", experimentDuration, restartThreshold);
  }

  public int getExperimentDuration() {
    return this.experimentDuration;
  }

  public int getRestartThreshold() {
    return this.restartThreshold;
  }

  /**
   * Performs clean up before shut down.
   */
  public void cleanUp() throws IOException {
    if (!Strings.isNullOrEmpty(checkpointFile.get())) {
      // Delete the checkpoint file
       AbstractFile file = fileFactory.forFile(checkpointFile.get());
      if (file != null && file.exists()) {
        file.delete();
      }
    }
  }

  /** Singleton class for the experiment cache stored in experiments */
  public class ExperimentCache extends InMemoryCache<Experiment> {

    private Experiment lastExperiment;

    /**
     * Makes a new experiment with the given subject ID and caches it. The
     * subjects for the given {@code subjectIds} should have been cached already
     * before calling this method.
     *
     * @param subjectIds subject IDs for this experiment
     */
    public synchronized Experiment make(final List<Long> subjectIds) throws IOException {
      lastExperiment = new Experiment(ExperimentDb.this, nextExperimentId(), subjectIds);

      if (!Strings.isNullOrEmpty(checkpointFile.get())) {
        saveExperiment(lastExperiment);
      }

      return register(lastExperiment);
    }

    /** Return the last experiment created */
    public synchronized Experiment getLast() {
      if (lastExperiment == null && !Strings.isNullOrEmpty(checkpointFile.get())) {
        // Read the stored last experiment from the checkpoint file.
        lastExperiment = readExperiment();
      }
      return lastExperiment;
    }

    /**
     * Saves the given experiment to the checkpoint file.
     */
    private void saveExperiment(Experiment experiment) throws IOException {
      // We write to a temp file and then rename it to avoid partial update
      // errors
      final String tempFilePath = String.format("%s.tmp", checkpointFile.get());
      final AbstractFile tempFile = fileFactory.forFile(tempFilePath);
      if (tempFile.exists()) {
        tempFile.delete();
      }
      final OutputLogStream out = outputLogStreamFactory
          .forStream(fileFactory.forFile(tempFilePath, "w").outputStreamFor());

      // First write out the experiment
      final ExperimentDbProtos.Experiment.Builder experimentBuilder =
          ExperimentDbProtos.Experiment.newBuilder();
      experimentBuilder.setId(experiment.getIdOfObject());
      final List<SubjectStateBridge> subjects = experiment.getSubjects();
      for (SubjectStateBridge subject : subjects) {
        experimentBuilder.addSubjectIds(subject.getIdOfObject());
      }
      ByteBuffer buf = ByteBuffer.wrap(experimentBuilder.build().toByteArray());
      out.write(buf);

      // Then write out all the subjects in the experiment
      final JvmFlag[] arguments = JvmFlag.values();
      for (final SubjectStateBridge subject : subjects) {
        final ExperimentDbProtos.Subject.Builder subjectBuilder =
            ExperimentDbProtos.Subject.newBuilder();
        subjectBuilder.setId(subject.getIdOfObject());

        // Copy the command line
        final CommandLine commandLine = subject.getCommandLine();
        final ExperimentDbProtos.CommandLine.Builder commandLineBuilder =
            ExperimentDbProtos.CommandLine.newBuilder();
        for (final JvmFlag argument : arguments) {
          final long value = commandLine.getValue(argument);
          final ExperimentDbProtos.CommandLineArgument.Builder argumentBuilder =
              ExperimentDbProtos.CommandLineArgument.newBuilder();

          argumentBuilder.setName(argument.name());
          argumentBuilder.setValue(String.valueOf(value));
          commandLineBuilder.addArgument(argumentBuilder);
        }
        subjectBuilder.setCommandLine(commandLineBuilder);
        buf = ByteBuffer.wrap(subjectBuilder.build().toByteArray());
        out.write(buf);
      }

      if (out != null) {
        out.close();
        if (tempFile.exists()) {
          final AbstractFile cpFile = fileFactory.forFile(checkpointFile.get());
          if (cpFile.exists()) {
            cpFile.delete();
          }

          log.info(
              String.format("Renaming temporary checkpoint file from '%s' to '%s'.", tempFile,
              checkpointFile.get()));

          tempFile.renameTo(checkpointFile.get());
          log.log(Level.INFO, "Checkpoint written sucessfully");
        }
      }
    }

    /**
     * Returns the last experiment saved in the checkpoint file. If none found,
     * returns null. The returned experiment is also cached.
     */
    private Experiment readExperiment() {
      InputLogStream in = null;
      Experiment experiment = null;
      try {
        in = inputLogStreamFactory
            .forStream(fileFactory.forFile(checkpointFile.get()).inputStreamFor());

        final Iterator<ByteBuffer> bufferIterator = in.iterator();
        if (!bufferIterator.hasNext()) {
          if (in != null) {
            in.close();
          }
          log.log(Level.INFO, "Checkpoint file is empty.");
          return null;
        }

        // First read the experiment
        final ExperimentDbProtos.Experiment.Builder experimentBuilder =
            ExperimentDbProtos.Experiment.newBuilder();
        experimentBuilder.mergeFrom(streamFromByteBuffer(bufferIterator.next()));
        final ExperimentDbProtos.Experiment exp = experimentBuilder.build();
        experiment = new Experiment(ExperimentDb.this, exp.getId(), exp.getSubjectIdsList());

        // Then read all the subjects
        final List<Long> subjectIds = Lists.newArrayList();
        while (bufferIterator.hasNext()) {
          final ExperimentDbProtos.Subject.Builder subjectBuilder =
              ExperimentDbProtos.Subject.newBuilder();
          subjectBuilder.mergeFrom(streamFromByteBuffer(bufferIterator.next()));

          final ExperimentDbProtos.Subject subject = subjectBuilder.build();

          // Cache the subject
          final SubjectStateBridge bridge = ExperimentDb.this.subjects.make(subject.getId());
          subjectIds.add(subject.getId());

          /* TODO(team): Migrate the stink that this switch statement is into a EnumMap or have
           *             JvmFlagSet translate the mapping itself.
           */

          final JvmFlagSet.Builder builder = JvmFlagSet.builder();

          final ExperimentDbProtos.CommandLine cl = subject.getCommandLine();
          for (final ExperimentDbProtos.CommandLineArgument arg : cl.getArgumentList()) {
            final int value = Integer.parseInt(arg.getValue());
            final JvmFlag argument = JvmFlag.valueOf(arg.getName());
            switch (argument) {

              case ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR:
                builder.withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, value);
                break;
              case CMS_EXP_AVG_FACTOR:
                builder.withValue(JvmFlag.CMS_EXP_AVG_FACTOR, value);
                break;
              case CMS_INCREMENTAL_DUTY_CYCLE:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, value);
                break;
              case CMS_INCREMENTAL_DUTY_CYCLE_MIN:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, value);
                break;
              case CMS_INCREMENTAL_OFFSET:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, value);
                break;
              case CMS_INCREMENTAL_SAFETY_FACTOR:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, value);
                break;
              case CMS_INITIATING_OCCUPANCY_FRACTION:
                builder.withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, value);
                break;
              case GC_TIME_RATIO:
                builder.withValue(JvmFlag.GC_TIME_RATIO, value);
                break;
              case HEAP_SIZE:
                builder.withValue(JvmFlag.HEAP_SIZE, value);
                break;
              case MAX_GC_PAUSE_MILLIS:
                builder.withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, value);
                break;
              case MAX_HEAP_FREE_RATIO:
                builder.withValue(JvmFlag.MAX_HEAP_FREE_RATIO, value);
                break;
              case MIN_HEAP_FREE_RATIO:
                builder.withValue(JvmFlag.MIN_HEAP_FREE_RATIO, value);
                break;
              case NEW_RATIO:
                builder.withValue(JvmFlag.NEW_RATIO, value);
                break;
              case NEW_SIZE:
                builder.withValue(JvmFlag.NEW_SIZE, value);
                break;
              case MAX_NEW_SIZE:
                builder.withValue(JvmFlag.MAX_NEW_SIZE, value);
                break;
              case PARALLEL_GC_THREADS:
                builder.withValue(JvmFlag.PARALLEL_GC_THREADS, value);
                break;
              case SOFT_REF_LRU_POLICY_MS_PER_MB:
                builder.withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, value);
                break;
              case SURVIVOR_RATIO:
                builder.withValue(JvmFlag.SURVIVOR_RATIO, value);
                break;
              case TENURED_GENERATION_SIZE_INCREMENT:
                builder.withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, value);
                break;
              case YOUNG_GENERATION_SIZE_INCREMENT:
                builder.withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, value);
                break;
              case USE_CONC_MARK_SWEEP_GC:
                builder.withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, value);
                break;
              case USE_PARALLEL_GC:
                builder.withValue(JvmFlag.USE_PARALLEL_GC, value);
                break;
              case USE_PARALLEL_OLD_GC:
                builder.withValue(JvmFlag.USE_PARALLEL_OLD_GC, value);
                break;
              case USE_SERIAL_GC:
                builder.withValue(JvmFlag.USE_SERIAL_GC, value);
                break;
              case CMS_INCREMENTAL_MODE:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_MODE, value);
                break;
              case CMS_INCREMENTAL_PACING:
                builder.withValue(JvmFlag.CMS_INCREMENTAL_PACING, value);
                break;
              case USE_CMS_INITIATING_OCCUPANCY_ONLY:
                builder.withValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY, value);
                break;
            }
          }

          final JvmFlagSet jvmFlagSet = builder.build();

          bridge.storeCommandLine(jvmFlagSet);
        }
        experiment.setSubjectIds(subjectIds);
      } catch (IOException ignore) {
        log.log(
          Level.INFO, "Unable to open the checkpoint file due to " + ignore.getMessage(), ignore);
        // If we can't read the saved experiment, just return null.
        return null;
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (final IOException e) {
            log.log(Level.SEVERE, "Could not close checkpoint file.", e);
          }
        }
      }

      return register(experiment);
    }
  }

  /** Generate a new Experiment ID. */
  public synchronized long nextExperimentId() {
    currentExperimentId++;
    write("nextExperimentId(local)", currentExperimentId);
    return currentExperimentId;
  }

  public synchronized long getExperimentId() {
    return currentExperimentId;
  }

  /** Singleton class for the subject cache stored in subjects */
  public class SubjectCache extends InMemoryCache<SubjectStateBridge> {
    /**
     * Make a new subject and cache it.
     */
    public SubjectStateBridge make() {
      return make(nextSubjectId());
    }

    /**
     * Makes a new subject with the given ID and cache it.
     *
     * @param subjectId Subject ID
     */
    public SubjectStateBridge make(final long subjectId) {
      return register(new SubjectStateBridge(ExperimentDb.this, subjectId));
    }
  }

  /** Generate a new Subject ID */
  private synchronized long nextSubjectId() {
    final long id = ++localSubjectId;
    write("nextSubjectId(local)", id);
    return id;
  }

  /**
   * @return the experiments
   */
  public ExperimentCache getExperiments() {
    return experiments;
  }

  private InputStream streamFromByteBuffer(final ByteBuffer buffer) {
    return new InputStream() {

      @Override
      public int read() {
        return buffer.hasRemaining() ? buffer.get() : -1;
      }
    };
  }
}
