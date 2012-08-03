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
 * A {@code ProcessInvoker} is used to invoke external processes.
 */
public interface ProcessInvoker {

  /**
   * Invoke an external process asynchronously.
   * @param cmdArgs The command and arguments to invoke. This array must
   * be non-{@code null} and have length at least 1. The 0th element of the
   * array should be the command and the remaining elements should be the
   * arguments
   * @return A {@link ExternalProcess} that may be used to keep track
   * of the asynchronous process.
   * @throws CommandExecutionException If there is a problem executing
   * the command.
   */
  public ExternalProcess invoke(String[] cmdArgs) throws CommandExecutionException;

  /**
   * Invoke an external process, wait for it to complete normally, and return
   * a {@link BufferedReader} from which the standard out of the process
   * may be read.
   * @param cmdArgs The command and arguments to invoke. This array must
   * be non-{@code null} and have length at least 1. The 0th element of the
   * array should be the command and the remaining elements should be the
   * arguments
   * @param maxWaitTime The maximum time, in milliseconds, that we should
   * wait for the process to complete. If this value is non-positive
   * then we will wait forever.
   * @return A {@link BufferedReader} from which the standard out
   * of the process may be read
   * @throws CommandTimeoutException If {@code maxWaitTime > 0}
   * and the process does not complete in time.
   * @throws CommandExecutionException If there is a problem executing
   * the command, or if the command executes but returns a non-zero value.
   */
  public BufferedReader invokeAndWait(String[] cmdArgs, long maxWaitTime)
      throws CommandExecutionException;

}
