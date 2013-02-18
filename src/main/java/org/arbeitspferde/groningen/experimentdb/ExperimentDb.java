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
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
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

  public void reset(ExperimentDb anotherDb) {
    experiments.reset(anotherDb.experiments);
    subjects.reset(anotherDb.subjects);
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
  public void cleanUp() {
  }

  /** Singleton class for the experiment cache stored in experiments */
  public class ExperimentCache extends InMemoryCache<Experiment> {

    private Experiment lastExperiment;

    @Override
    public void reset(InMemoryCache<Experiment> anotherCache) {
      super.reset(anotherCache);
      this.lastExperiment = ((ExperimentCache) anotherCache).lastExperiment;
    }
    
    public synchronized Experiment lastExperiment() {
      return lastExperiment;
    }
    
    /**
     * Makes a new experiment with the given subject ID and caches it. The
     * subjects for the given {@code subjectIds} should have been cached already
     * before calling this method.
     *
     * @param subjectIds subject IDs for this experiment
     */
    public synchronized Experiment make(final List<Long> subjectIds) {
      lastExperiment = new Experiment(ExperimentDb.this, nextExperimentId(), subjectIds);

      return register(lastExperiment);
    }

    /** Return the last experiment created */
    public synchronized Experiment getLast() {
      return lastExperiment;
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
