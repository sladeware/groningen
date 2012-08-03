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

import com.google.common.base.Joiner;

import org.arbeitspferde.groningen.externalprocess.CommandExecutionException;
import org.arbeitspferde.groningen.externalprocess.ExternalProcess;
import org.arbeitspferde.groningen.externalprocess.ProcessInvoker;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestingProcessInvoker implements ProcessInvoker {

  public List<String> receivedCommands = new LinkedList<String>();
  public Map<String, String> mockResponses = new HashMap<String, String>(10);

  public ExternalProcess invoke(String[] cmdArgs) throws CommandExecutionException {
    receivedCommands.add(Joiner.on(' ').join(cmdArgs));
    return null;
  }

  public BufferedReader invokeAndWait(String[] cmdArgs, long maxWaitTime)
      throws CommandExecutionException {
    String commandLine = Joiner.on(' ').join(cmdArgs);
    receivedCommands.add(commandLine);
    String response = mockResponses.get(commandLine);
    if (response == null) {
      throw new RuntimeException("Received unexpected command line: " + commandLine);
    }
    return new BufferedReader(new StringReader(response));
  }
}
