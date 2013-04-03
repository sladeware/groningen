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

import org.arbeitspferde.groningen.config.GroningenConfig;

/**
 * The SubjectRestart class contains the number of subject restarts during an experiment that is
 * cached by subjectId. You can use restartThresholdCrossed(...) to find out if a subject crossed
 * the threshold.
 */
public class SubjectRestart {
  /** The number of times this subject was restarted during the experiment */
  private long numberOfRestarts = 0;

  /** The last time this subject was restarted */
  private long lastRestartTime = 0;

  /** Flag indicating that when true indicates this subject did not run in production */
  private boolean didNotRun = true;



  /** Creation by package only */
  SubjectRestart() { }

  /** Yet another restart! */
  public void anotherRestart() {
    this.numberOfRestarts++;
  }

  /** Returns true when the subject did not run in production for an experiment */
  public boolean didNotRun() {
    return this.didNotRun;
  }

  public long getLastRestartTime() {
    return this.lastRestartTime;
  }

  /** Returns number of restarts for a subject */
  public long getNumberOfRestarts() {
    return this.numberOfRestarts;
  }

  /** Returns whether or not the restart threshold has been crossed by the input subject */
  public boolean restartThresholdCrossed(GroningenConfig config) {
    return this.numberOfRestarts >= config.getParamBlock().getRestart();
  }

  public void setLastRestartTime(long lastRestartTime) {
    this.lastRestartTime = lastRestartTime;
  }

  /** Call this method to indicate this subject was started in production */
  public void subjectStarted() {
    this.didNotRun = false;
  }
}
