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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.arbeitspferde.groningen.security.VendorSecurityManager;
import org.arbeitspferde.groningen.security.VendorSecurityManager.PathPermission;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Singleton
public class CmdProcessInvoker implements ProcessInvoker {
  private static final Logger logger = Logger.getLogger(CmdProcessInvoker.class.getName());
  private final AtomicLong subprocessSuccessfulInvocations = new AtomicLong();
  private final AtomicLong subprocessFailedInvocations = new AtomicLong();
  private final AtomicLong subprocessTimeoutInvocations = new AtomicLong();

  private final VendorSecurityManager securityManager;

  @Inject
  public CmdProcessInvoker(final MetricExporter metricExporter,
      final VendorSecurityManager securityManager) {
    metricExporter.register(
        "subprocess_successful_invocations_total",
        "The number of times that subprocesses have been successfully executed.",
        Metric.make(subprocessSuccessfulInvocations));
    metricExporter.register(
        "subprocess_failed_invocations_total",
        "The number of times that subprocess have failed to execute.",
        Metric.make(subprocessFailedInvocations));
    metricExporter.register(
        "subprocess_timeout_invocations_total",
        "The number of times that subprocess have timed out during execution.",
        Metric.make(subprocessTimeoutInvocations));

    this.securityManager = securityManager;
  }

  private class CmdProcess implements ExternalProcess {
    private Process process;
    private Integer exitValue = null;

    public CmdProcess(Process process) {
      this.process = process;
    }

    public boolean isCompleted() {
      try {
        process.exitValue();
        return true;
      } catch (IllegalThreadStateException e) {
        return false;
      }
    }

    public BufferedReader getStandardOut() {
      return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public int exitValue() {
      if (exitValue == null) {
        this.exitValue = process.exitValue();
        if (exitValue == 0) {
          subprocessSuccessfulInvocations.incrementAndGet();
        } else {
          subprocessFailedInvocations.incrementAndGet();
        }
      }
      return exitValue;
    }
  }

  private ExternalProcess invoke(String[] cmdArgs, String cmdArgsString)
      throws CommandExecutionException {
    try {
      logger.info(String.format("invoking command: %s", cmdArgsString));
      securityManager.applyPermissionToPathForClass(
          PathPermission.EXECUTE_FILESYSTEM_ENTITY, "./-", this.getClass());
      securityManager.applyPermissionToPathForClass(
          PathPermission.EXECUTE_FILESYSTEM_ENTITY, "/bin/bash", this.getClass());
      Process process = Runtime.getRuntime().exec(cmdArgs);
      return new CmdProcess(process);
    } catch (IOException e) {
      throw new CommandExecutionException(e);
    }
  }

  public ExternalProcess invoke(String[] cmdArgs) throws CommandExecutionException {
    String cmdArgsString = Joiner.on(' ').join(cmdArgs);
    return invoke(cmdArgs, cmdArgsString);
  }

  public BufferedReader invokeAndWait(String[] cmdArgs, long maxWaitMillis)
      throws CommandExecutionException {
    String cmdArgsString = Joiner.on(' ').join(cmdArgs);
    ExternalProcess process = invoke(cmdArgs, cmdArgsString);
    long startTime = System.currentTimeMillis();
    long sleepMillis = 1000;
    if (maxWaitMillis > 0) {
      // If maxWaitMillis is being used, set it to at least
      // 100 milliseconds.
      maxWaitMillis = Math.max(maxWaitMillis, 100);
      // If the process is not done yet, sleep for a second,
      // unless maxWaitMillis is less than one whole second.
      sleepMillis = Math.min(1000, maxWaitMillis);
    }
    while (!process.isCompleted()) {
      logger.info(String.format("Waiting for for %s", cmdArgsString));
      try {
        Thread.sleep(sleepMillis);
      } catch (InterruptedException e) {
        throw new CommandExecutionException(e);
      }
      if (maxWaitMillis > 0) {
        long now = System.currentTimeMillis();
        if (now - startTime > maxWaitMillis) {
          subprocessTimeoutInvocations.incrementAndGet();
          throw new CommandTimeoutException();
        }
      }
    }
    int exitValue = process.exitValue();
    if (exitValue > 0) {
      throw new CommandExecutionException("The exit value was " + exitValue + ". " + cmdArgsString);
    }
    return process.getStandardOut();
  }
}
