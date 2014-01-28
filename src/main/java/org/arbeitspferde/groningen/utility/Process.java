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

package org.arbeitspferde.groningen.utility;


import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides primitives for managing the system's processes.
 */
public class Process {
  // TODO(drk): rewrite class implementation with help of JNI to support Windows and Linux.

  private static final Logger log = Logger.getLogger(Process.class.getCanonicalName());

  private static final Pattern STAT_FILE_FORMAT = Pattern.compile(
      "^([0-9-]+)\\s([^\\s]+)\\s[^\\s]\\s([0-9-]+)\\s([0-9-]+)\\s([0-9-]+)\\s([0-9-]+\\s){16}" +
      "([0-9]+)(\\s[0-9-]+){16}");

  /**
   * This class encapsulates process information.
   */
  public static class ProcessInfo {
    private Integer processId;
    private Integer processGroupId;
    private long startTime;
    private String commandLine;

    public ProcessInfo(int processId, int processGroupId, long startTime, String commandLine) {
      this.processId = Integer.valueOf(processId);
      this.processGroupId = Integer.valueOf(processGroupId);
      this.startTime = startTime;
      this.commandLine = commandLine;
    }

    public Integer getProcessId() {
      return this.processId;
    }

    public Integer getProcessGroupId() {
      return this.processGroupId;
    }

    /**
     * Returns the creation time of the process in seconds since the start of the epoch.
     *
     * @return a long that represents process creation time.
     */
    public long getStartTime() {
      return this.startTime;
    }

    public String getCommandLine() {
      return this.commandLine;
    }
  }

  /**
   * Is the process with identifier {@code processId} still alive?
   *
   * This method assumes that the process with identifier {@code processId} was alive not too long
   * ago, and hence assumes no chance of pid-wrapping-around.
   *
   * @param processId Process identifier to check.
   * @return {@code true} if process is alive.
   */
  public static final boolean isAlive(int processId) {
    java.lang.Process process = null;
    String[] command = {"kill", "-0", Integer.toString(processId)};
    try {
      process = Runtime.getRuntime().exec(command);
    } catch (IOException e) {
      log.log(Level.WARNING, "Error executing shell command " + Arrays.toString(command) + e);
      return false;
    }
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      return false;
    }
    return (process.exitValue() == 0 ? true : false);
  }

  /**
   * @param the identifier of this process or 0 if it cannot be determined.
   */
  public static final int myProcessId() {
    int processId = 0;
    try {
      processId = Integer.parseInt(new File("/proc/self").getCanonicalFile().getName());
    } catch (NumberFormatException e) {
      log.log(Level.WARNING, "Cannot parse PID: " + e);
    } catch (IOException e) {
      log.log(Level.WARNING, "PID file cannot be processed: " + e);
    }
    return processId;
  }

  /**
   * @param processId the PID of the target process.
   * @return the {@class ProcessInfo} instance or {@code null} if process information can not be
   *         obtained.
   */
  public static final ProcessInfo getProcessInfo(int processId) {
    File statStream = null;
    ProcessInfo info = null;
    FileReader statReader = null;
    BufferedReader statDataReader = null;
    try {
      statStream = new File("/proc/" + Integer.toString(processId) + "/stat");
      statReader = new FileReader(statStream);
      statDataReader = new BufferedReader(statReader);
    } catch (FileNotFoundException e) {
      log.log(Level.WARNING, "PID file doesn't exist: " +
          "/proc/" + Integer.toString(processId) + "/stat");
      return info;
    }
    try {
      String line = statDataReader.readLine();
      Matcher matcher = STAT_FILE_FORMAT.matcher(line);
      if (matcher.find()) {
        String commandLine = Files.toString(new File("/proc/" + Integer.toString(processId) +
                "/cmdline"), StandardCharsets.UTF_8);
        // TODO(drk): read the start time from the stat file.
        info = new ProcessInfo(processId, Integer.parseInt(matcher.group(4)),
            statStream.lastModified() / 1000, commandLine);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, "Error reading the stream: " + e);
    } finally {
      try {
        statReader.close();
        statDataReader.close();
      } catch (IOException e) {
        log.log(Level.WARNING, "Cannot close the stream");
      }
    }
    return info;
  }

  /**
   * @return the group identifier of this process.
   */
  public static final int myProcessGroupId() {
    ProcessInfo info = getProcessInfo(myProcessId());
    return info.getProcessGroupId();
  }

  /**
   * @param processGroupId the process group identifier of the target process group.
   * @return a list of process identifiers that have same group process group identifier.
   */
  public static final List<Integer> getProcessIdsOf(int processGroupId) {
    File[] procDir = (new File("/proc")).listFiles();
    List<Integer> processIds = new ArrayList();
    for (File processDir : procDir) {
      int processId;
      try {
        processId = Integer.parseInt(processDir.getName());
      } catch (NumberFormatException e) {
        continue;
      }
      ProcessInfo info = getProcessInfo(processId);
      if (info != null && info.getProcessGroupId() == processGroupId) {
        processIds.add(processId);
      }
    }
    return processIds;
  }

  public static final void restartProcessGroup(int processGroupId) {
    // TODO(drk): implement this.
    log.log(Level.INFO, "Restart group of processes " + processGroupId);
  }

  public static final void restartProcess(int processId) {
    // TODO(drk): implement this.
    log.log(Level.INFO, "Restart process " + processId);
  }
}
