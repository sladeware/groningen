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

package org.arbeitspferde.groningen.externalprocess;

import java.io.BufferedReader;

/**
 * Represents an external process that was started via
 * {@link ProcessInvoker#invoke(String[])}
 */
public interface ExternalProcess {

  /**
   * Returns whether or not the process has completed.
   * @return whether or not the process has completed.
   */
  public boolean isCompleted();


  /**
   * Returns a {@link BufferedReader} from which one may read
   * the standard out of the process.
   * @return a {@link BufferedReader}
   */
  public BufferedReader getStandardOut();


  /**
   * Returns the exit value for the subprocess.
   *
   * @return the exit value of the process represented by this {@code
   *         ExternalProcess} object. By convention, the value 0 indicates
   *         normal termination.
   * @throws IllegalThreadStateException if the subprocess represented by this
   *         Process object has not yet terminated.
   */
  public int exitValue() throws IllegalThreadStateException;
}
