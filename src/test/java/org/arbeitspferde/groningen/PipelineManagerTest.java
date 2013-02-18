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
import junit.framework.TestCase;
import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.SimpleScope;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

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

  private PipelineManager pipelineManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    pipelineMock = EasyMock.createNiceMock(Pipeline.class);
    dataStoreMock = EasyMock.createNiceMock(Datastore.class);
    pipelineIdGeneratorMock = EasyMock.createNiceMock(PipelineIdGenerator.class);
    pipelineScope = new SimpleScope();

    Provider<Pipeline> pipelineProvider = new Provider<Pipeline>() {
      @Override
      public Pipeline get() {
        return pipelineMock;
      }
    };

    pipelineManager = new PipelineManager(pipelineIdGeneratorMock, pipelineScope,
        pipelineProvider, dataStoreMock);
  }

  /**
   * Test that when Pipeline is created in non-blocking mode, it eventually gets executed
   * and we can find it by its' pipeline id.
   */
  public void testStartsPipelineInNonBlockingMode() throws InterruptedException {
    ConfigManager configManager = new StubConfigManager();

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

    EasyMock.replay(pipelineMock, pipelineIdGeneratorMock);

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
    ConfigManager configManager = new StubConfigManager();

    EasyMock.expect(
        pipelineIdGeneratorMock.generatePipelineId(EasyMock.anyObject(GroningenConfig.class))).
          andReturn(new PipelineId("pipelineId"));

    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        Thread.sleep(500);
        return null;
      }});

    EasyMock.replay(pipelineMock, pipelineIdGeneratorMock);

    PipelineId id = pipelineManager.startPipeline(configManager, true);
    assertNotNull(pipelineManager.findPipelineById(id));
  }
}