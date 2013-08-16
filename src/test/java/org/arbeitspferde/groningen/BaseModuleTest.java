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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import junit.framework.TestCase;
import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.SystemAdapter;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.Displayable;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.eventlog.EventLoggerService;
import org.arbeitspferde.groningen.eventlog.SafeProtoLoggerFactory;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.externalprocess.ProcessInvoker;
import org.arbeitspferde.groningen.generator.Generator;
import org.arbeitspferde.groningen.open.OpenModule;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.arbeitspferde.groningen.subject.SubjectInterrogator;
import org.arbeitspferde.groningen.utility.Clock;
import org.easymock.EasyMock;

import java.util.Timer;

/**
 * Tests for {@link BaseModule}.
 */
public class BaseModuleTest extends TestCase {
  private Injector injector;
  private BlockScope pipelineScope;
  private BlockScope pipelineIterationScope;
  private PipelineId pipelineId;
  private GroningenConfig stubConfig;

  @Override
  public void setUp() {
    final String[] args = new String[] {};

    injector = Guice.createInjector(new OpenModule(), new BaseModule(args),
        new GroningenConfigParamsModule(), new ServicesModule());
    pipelineScope = injector.getInstance(Key.get(BlockScope.class,
        Names.named(PipelineScoped.SCOPE_NAME)));
    pipelineIterationScope = injector.getInstance(Key.get(BlockScope.class,
        Names.named(PipelineIterationScoped.SCOPE_NAME)));
    pipelineId = new PipelineId("pipelineId");
    stubConfig = new StubConfigManager.StubConfig() {
      @Override
      public GroningenParamsOrBuilder getParamBlock() {
        return GroningenParams.getDefaultInstance().toBuilder();
      }
    };
  }

  public void testInjectorProvision() {
    assertNotNull(injector);
  }

  public void testInjector_ProvisionClock() {
    final Clock clock = injector.getInstance(Clock.class);

    assertNotNull(clock);
  }

  public void testInjector_ProvisionClockAsSingleton() {
    final Clock clock1 = injector.getInstance(Clock.class);
    final Clock clock2 = injector.getInstance(Clock.class);

    assertEquals(clock1, clock2);
  }

  public void testInjector_ProvisionProcessInvoker() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      pipelineIterationScope.enter();
      GroningenConfigParamsModule.nailConfigToScope(stubConfig, pipelineIterationScope);

      try {
        final ProcessInvoker processInvoker = injector.getInstance(ProcessInvoker.class);
        assertNotNull(processInvoker);
      } finally {
        pipelineIterationScope.exit();
      }
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionDisplayMediator() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      final DisplayMediator displayMediator = injector.getInstance(DisplayMediator.class);

      assertNotNull(displayMediator);
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionDisplayable() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      final Displayable displayable = injector.getInstance(Displayable.class);

      assertNotNull(displayable);
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionMonitor() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      final MonitorGroningen monitor = injector.getInstance(MonitorGroningen.class);

      assertNotNull(monitor);
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionMonitorAndDisplayableAsPerPipelineObjects() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      final MonitorGroningen monitor = injector.getInstance(MonitorGroningen.class);
      final Displayable displayable = injector.getInstance(Displayable.class);

      assertSame(monitor, displayable);
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionExperimentDb() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      final ExperimentDb experimentDb = injector.getInstance(ExperimentDb.class);

      assertNotNull(experimentDb);
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionGenerator() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      pipelineIterationScope.enter();
      GroningenConfigParamsModule.nailConfigToScope(stubConfig, pipelineIterationScope);

      try {
        final Generator generator = injector.getInstance(Generator.class);
        assertNotNull(generator);
      } finally {
        pipelineIterationScope.exit();
      }
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionSubjectInterrogator() {
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);

      pipelineIterationScope.enter();
      GroningenConfigParamsModule.nailConfigToScope(stubConfig, pipelineIterationScope);
      try {
        final SubjectInterrogator subjectInterrogator =
            injector.getInstance(SubjectInterrogator.class);

        assertNotNull(subjectInterrogator);
      } finally {
        pipelineIterationScope.exit();
      }
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionSystemAdapter() {
    final SystemAdapter systemAdapter = injector.getInstance(SystemAdapter.class);

    assertNotNull(systemAdapter);
  }

  public void testInjector_ProvisionSafeProtoLoggerFactory() {
    final SafeProtoLoggerFactory safeProtoLoggerFactory =
        injector.getInstance(SafeProtoLoggerFactory.class);

    assertNotNull(safeProtoLoggerFactory);
  }

  public void testInjector_ProvisionEventLoggerService() {
    final EventLoggerService eventLoggerService = injector.getInstance(EventLoggerService.class);

    assertNotNull(eventLoggerService);
  }

  public void testInjector_ProvisionDaemonTimerProvider() {
    final Provider<Timer> daemonTimerProvider = injector.getProvider(Timer.class);

    assertNotNull(daemonTimerProvider);
  }

  public void testInjector_DaemonTimerProvider_ProvisionsTimer() {
    final Provider<Timer> daemonTimerProvider = injector.getProvider(Timer.class);

    assertNotNull(daemonTimerProvider.get());
  }

  public void testInjector_DaemonTimerProvider_ProvisionsUniqueTimers() {
    final Provider<Timer> daemonTimerProvider = injector.getProvider(Timer.class);

    final Timer first = daemonTimerProvider.get();
    final Timer second = daemonTimerProvider.get();

    assertNotSame(first, second);
  }

  public void testInjector_ProvisionPipelineIdGenerator() {
    final PipelineIdGenerator pipelineIdGenerator =
        injector.getInstance(PipelineIdGenerator.class);

    assertNotNull(pipelineIdGenerator);
  }

  public void testInjector_ProvisionPipeline() {
    final ConfigManager mockConfigManager =
        EasyMock.createNiceMock(ConfigManager.class);
    final PipelineSynchronizer mockSynchronizer =
        EasyMock.createNiceMock(PipelineSynchronizer.class);
    final PipelineStageInfo mockPipelineStageInfo =
        EasyMock.createNiceMock(PipelineStageInfo.class);
    EasyMock.replay(mockConfigManager);
    EasyMock.replay(mockSynchronizer);

    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineId.class, pipelineId);
      pipelineScope.seed(ConfigManager.class, mockConfigManager);
      pipelineScope.seed(PipelineSynchronizer.class, mockSynchronizer);
      pipelineScope.seed(PipelineStageInfo.class, mockPipelineStageInfo);

      assertNotNull(injector.getInstance(Pipeline.class));
    } finally {
      pipelineScope.exit();
    }
  }

  public void testInjector_ProvisionConfigManagerProvider() {
    final Provider<ConfigManager> configManagerProvider = injector.getProvider(ConfigManager.class);

    assertNotNull(configManagerProvider);
  }


  public void testInjector_ProduceStartTime() {
    final Long startTime = injector.getInstance(Key.get(Long.class, Names.named("startTime")));

    assertNotNull(startTime);
  }

  public void testInjector_ProvisionArgs() {
    final String[] args = injector.getInstance(String[].class);

    assertNotNull(args);
  }
  
  public void testInjector_ProducePipelineStageInfoInScopes() {
    PipelineStageInfo pipelineStageInfo = new PipelineStageInfo();
    
    pipelineScope.enter();
    try {
      pipelineScope.seed(PipelineStageInfo.class, pipelineStageInfo);
      final PipelineStageInfo pipelineScopedInfo = injector.getInstance(PipelineStageInfo.class);
      
      pipelineIterationScope.enter();
      try {
        final PipelineStageInfo pipelineIterationScopedInfo =
            injector.getInstance(PipelineStageInfo.class);
        assertNotNull(pipelineScopedInfo);
        assertEquals(pipelineScopedInfo, pipelineIterationScopedInfo);
      } finally {
        pipelineIterationScope.exit();
      }
    } finally {
      pipelineScope.exit();
    }
  }
}
