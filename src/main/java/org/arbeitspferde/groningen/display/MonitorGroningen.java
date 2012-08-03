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

package org.arbeitspferde.groningen.display;

import org.arbeitspferde.groningen.common.EvaluatedSubject;

/**
 * Provides a unified interface to monitor Groningen. It stores and encapsulates the
 * Groningen objects to be displayed on the HUD.
 */
public interface MonitorGroningen {
  /**
   * Closes out accounting on a generation and starts accounting on a new
   * generation.
   */
  public void processGeneration();

  /**
   * Adds an {@link EvaluatedSubject}. The interface does not define how the
   * representation will be laid out. Layout details left to implementing
   * classes.
   */
  public void addIndividual(EvaluatedSubject evaluatedSubject);

  /** Adds a warning to the warning list */
  public void addWarning(String warning);

  /**
   * Determines the maximum number of distinct individuals we care about
   */
  public void maxIndividuals(int max);

  /**
   * Adds an {@link Object} to be monitored, with a description string. The
   * interface does not define how the representation will be laid out. Layout
   * details left to implementing classes.
   */
  public void monitorObject(Object obj, String infoString);

  /**
   * Given a monitored {@link Object}, the class stops monitoring it. It
   * {@code true} if the object was being monitored. The interface does not
   * define how the representation will be laid out. Layout details left to
   * implementing classes.
   */
  public boolean stopMonitoringObject(Object obj);

}
