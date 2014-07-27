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
import com.google.common.base.Preconditions;

import org.arbeitspferde.groningen.config.PipelineScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The ExperimentDb contains Groningen data generated and used by each of the major Groningen
 * components.  The primary purpose of the Experiment Database is to hold pipeline data that is
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

  private static final Joiner spaceSeparator = Joiner.on(" ");
  private static final Joiner commaSpaceSeparator = Joiner.on(", ");

  /** A subject id counter */
  private long localSubjectId = 0;

  /**
   * The current ID of experiments.  It is guaranteed to be unique per program invocation and
   * hosting address but nothing more.
   */
  private long currentExperimentId = 0;

  /** Experiments of this run */
  private Experiment lastExperiment; 
  private Map<Long, SubjectStateBridge> subjects = new HashMap<>();

  public void reset(ExperimentDb anotherDb) {
    localSubjectId = anotherDb.localSubjectId;
    currentExperimentId = anotherDb.currentExperimentId;
    lastExperiment = anotherDb.lastExperiment;
    subjects = new HashMap<>(anotherDb.subjects);
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

  public synchronized Experiment makeExperiment(final List<Long> subjectIds) {
    lastExperiment = new Experiment(ExperimentDb.this, nextExperimentId(), subjectIds);
    return lastExperiment;
  }

  public synchronized Experiment getLastExperiment() {
    return lastExperiment;
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
  
  public SubjectStateBridge makeSubject() {
    return makeSubject(nextSubjectId());
  }
  
  public SubjectStateBridge makeSubject(final long subjectId) {
    SubjectStateBridge bridge = new SubjectStateBridge(ExperimentDb.this, subjectId);
    subjects.put(subjectId, bridge);
    return bridge;
  }
  
  public SubjectStateBridge lookupSubject(final long subjectId) {
    Preconditions.checkArgument(subjectId >= 0, "subjectId should be > 0");
    return subjects.get(subjectId);
  }

  /** Generate a new Subject ID */
  private synchronized long nextSubjectId() {
    final long id = ++localSubjectId;
    write("nextSubjectId(local)", id);
    return id;
  }
}
