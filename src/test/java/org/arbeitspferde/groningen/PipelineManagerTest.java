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

import com.google.inject.Provider;

import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.SimpleScope;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.proto.Params.GroningenParams.PipelineSynchMode;
import org.arbeitspferde.groningen.scorer.HistoricalBestPerformerScorer;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Test for {@link PipelineManager}
 *
 * @author mbushkov@google.com (Mikhail Bushkov)
 */
public class PipelineManagerTest extends TestCase {
  private BlockScope pipelineScope;
  private Pipeline pipelineMock;
  private Datastore dataStoreMock;
  private PipelineIdGenerator pipelineIdGeneratorMock;
  private Provider<Pipeline> pipelineProvider;
  private Map<PipelineSynchMode, Provider<PipelineSynchronizer>> pipelineSyncProviderMap;
  private PipelineSynchronizer defaultPipelineSynchronizer;
  private PipelineManager pipelineManager;
  private HistoricalBestPerformerScorer bestPerformerScorer;
  private Provider<HistoricalBestPerformerScorer> bestPerformerScorerProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    pipelineMock = EasyMock.createNiceMock(Pipeline.class);
    dataStoreMock = EasyMock.createNiceMock(Datastore.class);
    pipelineIdGeneratorMock = EasyMock.createNiceMock(PipelineIdGenerator.class);
    pipelineScope = new SimpleScope();
    pipelineSyncProviderMap = new HashMap<>();
    defaultPipelineSynchronizer = new EmptyPipelineSynchronizer();
    bestPerformerScorer = EasyMock.createNiceMock(HistoricalBestPerformerScorer.class);

    Provider<PipelineSynchronizer> emptySynchronizerProvider =
        new Provider<PipelineSynchronizer>() {
      @Override
      public PipelineSynchronizer get() {
        return defaultPipelineSynchronizer;
      }
    };
    pipelineSyncProviderMap.put(PipelineSynchMode.NONE, emptySynchronizerProvider);

    pipelineProvider = new Provider<Pipeline>() {
      @Override
      public Pipeline get() {
        return pipelineMock;
      }

    };

    bestPerformerScorerProvider = new Provider<HistoricalBestPerformerScorer>() {
      @Override
      public HistoricalBestPerformerScorer get() {
        return bestPerformerScorer;
      }
    };

    pipelineManager = new PipelineManager(pipelineIdGeneratorMock, pipelineScope,
        pipelineProvider, dataStoreMock, pipelineSyncProviderMap, bestPerformerScorerProvider);
  }

  public void testGetRequestedPipelineSynchronizerWithDefaultParamBlock() {
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder().build();

    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);
    EasyMock.replay(config);

    PipelineSynchronizer synchronizer = pipelineManager.getRequestedPipelineSynchronizer(config);
    assertEquals(pipelineSyncProviderMap.get(PipelineSynchMode.NONE).get(), synchronizer);
  }

  public void testGetRequestedPipelineSynchronizerWithNamedSynchronizer() {
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setPipelineSyncType(GroningenParams.PipelineSynchMode.ITERATION_FINALIZATION_ONLY)
        .build();

    final PipelineSynchronizer finalizationSynchronizer = new IterationFinalizationSynchronizer();
    Provider<PipelineSynchronizer> localSynchronizerProvider =
        new Provider<PipelineSynchronizer>() {
      @Override
      public PipelineSynchronizer get() {
        return finalizationSynchronizer;
      }
    };
    pipelineSyncProviderMap.put(
        PipelineSynchMode.ITERATION_FINALIZATION_ONLY, localSynchronizerProvider);

    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);
    EasyMock.replay(config);

    PipelineSynchronizer synchronizer = pipelineManager.getRequestedPipelineSynchronizer(config);
    assertEquals(finalizationSynchronizer, synchronizer);
  }

  public void testGetRequestedPipelineSynchronizerWithNonexistantSynchronizer() {
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setPipelineSyncType(GroningenParams.PipelineSynchMode.BASIC_SEMAPHORE)
        .build();

    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);
    EasyMock.replay(config);

    PipelineSynchronizer synchronizer = pipelineManager.getRequestedPipelineSynchronizer(config);
    assertEquals(defaultPipelineSynchronizer, synchronizer);
  }

  /**
   * Test that when Pipeline is created in non-blocking mode, it eventually gets executed
   * and we can find it by its' pipeline id.
   */
  public void testStartsPipelineWithSpecificSynchronizerRequested() throws InterruptedException {
    ConfigManager configManager = EasyMock.createNiceMock(ConfigManager.class);
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setPipelineSyncType(GroningenParams.PipelineSynchMode.ITERATION_FINALIZATION_ONLY)
        .build();

    final PipelineSynchronizer finalizationSynchronizer = new IterationFinalizationSynchronizer();
    Provider<PipelineSynchronizer> localSynchronizerProvider =
        new Provider<PipelineSynchronizer>() {
      @Override
      public PipelineSynchronizer get() {
        return finalizationSynchronizer;
      }
    };
    pipelineSyncProviderMap.put(
        PipelineSynchMode.ITERATION_FINALIZATION_ONLY, localSynchronizerProvider);

    EasyMock.expect(configManager.queryConfig()).andReturn(config).anyTimes();
    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);

    PipelineId referenceId = new PipelineId("pipelineid");
    EasyMock.expect(
        pipelineIdGeneratorMock.generatePipelineId(EasyMock.anyObject(GroningenConfig.class))).
          andReturn(referenceId);

    BlockScope localPipelineScope = EasyMock.createMock(BlockScope.class);
    localPipelineScope.enter();
    localPipelineScope.seed(PipelineSynchronizer.class, finalizationSynchronizer);
    localPipelineScope.seed(PipelineId.class, referenceId);
    localPipelineScope.seed(ConfigManager.class, configManager);
    localPipelineScope.seed(
        EasyMock.same(PipelineStageInfo.class), EasyMock.anyObject(PipelineStageInfo.class));
    localPipelineScope.seed(HistoricalBestPerformerScorer.class, bestPerformerScorer);

    final ReentrantLock lock = new ReentrantLock();
    pipelineMock.run();
    localPipelineScope.exit();
    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        // Pausing pipeline until main thread explicitly unlocks the lock
        lock.lock();
        lock.unlock();
        return null;
      }});

    EasyMock.replay(configManager, config, pipelineMock, pipelineIdGeneratorMock,
        localPipelineScope, bestPerformerScorer);

    pipelineManager = new PipelineManager(pipelineIdGeneratorMock, localPipelineScope,
        pipelineProvider, dataStoreMock, pipelineSyncProviderMap, bestPerformerScorerProvider);

    lock.lock();
    try {
      PipelineId id = pipelineManager.startPipeline(configManager, false);
      Thread.sleep(500);
      assertNotNull(pipelineManager.findPipelineById(id));
    } finally {
      lock.unlock();
    }

    EasyMock.verify(localPipelineScope);
  }

  /**
   * Test that when Pipeline is created in non-blocking mode, it eventually gets executed
   * and we can find it by its' pipeline id.
   */
  public void testStartsPipelineInNonBlockingMode() throws InterruptedException {
    ConfigManager configManager = EasyMock.createNiceMock(ConfigManager.class);
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setPipelineSyncType(GroningenParams.PipelineSynchMode.NONE)
        .build();

    EasyMock.expect(configManager.queryConfig()).andReturn(config).anyTimes();
    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);
    EasyMock.expect(
        pipelineIdGeneratorMock.generatePipelineId(EasyMock.anyObject(GroningenConfig.class))).
          andReturn(new PipelineId("pipelineId"));

    final ReentrantLock lock = new ReentrantLock();
    pipelineMock.run();
    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        // Pausing pipeline until main thread explicitly unlocks the lock
        lock.lock();
        lock.unlock();
        return null;
      }});

    EasyMock.replay(configManager, config, pipelineMock, pipelineIdGeneratorMock);

    lock.lock();
    try {
      PipelineId id = pipelineManager.startPipeline(configManager, false);
      Thread.sleep(500);
      assertNotNull(pipelineManager.findPipelineById(id));
    } finally {
      lock.unlock();
    }
  }

  /**
   * Test that when Pipeline is created in blocking mode, we can find it by its' id immediately
   * after the startPipeline() call.
   */
  public void testStartsPipelineInBlockingMode() {
    ConfigManager configManager = EasyMock.createNiceMock(ConfigManager.class);
    GroningenConfig config = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setPipelineSyncType(GroningenParams.PipelineSynchMode.NONE)
        .build();

    EasyMock.expect(configManager.queryConfig()).andReturn(config).anyTimes();
    EasyMock.expect(config.getParamBlock()).andReturn(paramBlock);

    EasyMock.expect(
        pipelineIdGeneratorMock.generatePipelineId(EasyMock.anyObject(GroningenConfig.class))).
          andReturn(new PipelineId("pipelineId"));

    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        Thread.sleep(500);
        return null;
      }});

    EasyMock.replay(configManager, config, pipelineMock, pipelineIdGeneratorMock);

    PipelineId id = pipelineManager.startPipeline(configManager, true);
    assertNotNull(pipelineManager.findPipelineById(id));
  }
}
