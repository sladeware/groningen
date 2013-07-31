/* Copyright 2013 Google, Inc.
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

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;

/**
 * Loggers that record aspects of a given subject for subsequent analysis.
 * 
 * Slice and dice the results of an experimental pipeline, mainly intended for offline
 * analysis.
 */
public interface SubjectEventLogger {
  
  /**
   * Compose a logging entry for a given subject and log it.
   * 
   * @param config the active groningen config for this experiment iteration.
   * @param experiment the experiment iteration the subject is part of.
   * @param subject the subject to log. Expected to be complete and scored.
   */
  public void logSubjectInExperiment(GroningenConfig config, Experiment experiment,
      SubjectStateBridge subject);
}
