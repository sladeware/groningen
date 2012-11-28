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

package org.arbeitspferde.groningen;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.arbeitspferde.groningen.common.Settings;
import org.arbeitspferde.groningen.common.SystemAdapter;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.ProtoBufConfigManager;
import org.arbeitspferde.groningen.config.ProtoBufConfigManagerFactory;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Groningen is an autonomic Java performance optimization tool. This class is the
 * program entry point and main processing loop. It instantiates singletons that
 * represent stages in the iterative processing pipeline. Then it runs the
 * processing pipeline until Groningen decides to terminate.
 *
 */
@Singleton
public class GroningenWorkhorse implements Runnable {
  /** Logger for this class */
  private static final Logger log =
      Logger.getLogger(GroningenWorkhorse.class.getCanonicalName());

  private final Service backgroundServices;
  private final SystemAdapter systemAdapter;
  private final Settings settings;
  private final ProtoBufConfigManagerFactory protoBufConfigManagerFactory;
  private final PipelineManager pipelineManager;
  private final Build build;

  @Inject
  private GroningenWorkhorse(final Provider<Pipeline> pipelineProvider,
      final PipelineIdGenerator pipelineIdGenerator,
      final Service backgroundServices,
      final SystemAdapter systemAdapter,
      final Settings settings,
      final PipelineManager pipelineManager,
      final ProtoBufConfigManagerFactory protoBufConfigManagerFactory,
      final Build build) {
    this.backgroundServices = backgroundServices;
    this.systemAdapter = systemAdapter;
    this.pipelineManager = pipelineManager;
    this.settings = settings;
    this.protoBufConfigManagerFactory = protoBufConfigManagerFactory;
    this.build = build;
  }

  private ConfigManager createConfigManager(String source) {
    if (source == null) {
      throw new IllegalArgumentException("null source provided");
    }
    String [] sourceParts = source.split(":", 2);
    if (sourceParts[0].equals("proto")) {
      if (sourceParts.length < 2) {
        throw new IllegalArgumentException("provided source contained only proto spec, requires "
          + "additional information to instantiate the protobuf config manager");
      }

      ProtoBufConfigManager manager = protoBufConfigManagerFactory.forPath(sourceParts[1]);

      try {
        manager.initialize();
      } catch (IOException e) {
        throw new RuntimeException("I/O error with loading configuration.", e);
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException("Illegal configuration.", e);
      }
      return manager;
    } else if (source.startsWith("stub:")) {
      return new StubConfigManager();
    } else {
      throw new IllegalArgumentException(source + " not a recongized config type");
    }
  }

  private void startSubservices() {
    final int startupDeadline = settings.getStartupSubservicesDeadlineSeconds();

    log.info(
        String.format("Starting subservices; will wait %s seconds at most...", startupDeadline));
    try {
      final State state = backgroundServices.start().get(startupDeadline, TimeUnit.SECONDS);

      if (state != State.RUNNING) {
        log.severe(String.format(
            "After waiting %s seconds,  not all of the subservices could be started and were" +
                "in state %s.", startupDeadline, state));
        throw new RuntimeException("Not all subservices could be started.");
      }
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void stopSubservices() {
    try {
      final int shutdownDeadline = settings.getShutdownSubservicesDeadlineSeconds();
      log.info(String.format(
          "Waiting %s seconds for shutdown services to commence...", shutdownDeadline));
      backgroundServices.stop().get(shutdownDeadline, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Prepare for and then run the processing loop
   */
  public void run() {
    log.info(build.getStamp());

    try {
      startSubservices();

      ConfigManager configManager = createConfigManager(settings.getConfigFileName());

      PipelineId pipelineId = pipelineManager.startPipeline(configManager, true);
      Pipeline pipeline = pipelineManager.findPipelineById(pipelineId);
      if (pipeline == null) {
        throw new RuntimeException(String.format("Pipeline %s died almost immediately",
            pipelineId.toString()));
      }

      try {
        pipeline.joinPipeline();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      systemAdapter.exit(0);
    } catch (RuntimeException e) {
      log.log(Level.SEVERE, "Aborted.", e);
      try {
        stopSubservices();
      } catch (RuntimeException stopSubservicesError) {
        log.log(Level.SEVERE,
            "Could not shutdown subservices in a timely fashion.", stopSubservicesError);
      }
      systemAdapter.exit(1);
    }
  }
}
