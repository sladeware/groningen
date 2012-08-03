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

package org.arbeitspferde.groningen.config;


import junit.framework.TestCase;

import org.arbeitspferde.groningen.config.ProtoBufConfig;
import org.arbeitspferde.groningen.config.ProtoBufConfigManager;
import org.arbeitspferde.groningen.config.ProtoBufSource;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.easymock.EasyMock;

import java.io.IOException;

/**
 * Tests for {@link ProtoBufConfigManager}.
 */
public class ProtoBufConfigManagerTest extends TestCase {

  ProtoBufSource source;
  ProtoBufConfigManager.ProtoBufConfigFactory configFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    source = EasyMock.createMock(ProtoBufSource.class);
    configFactory = EasyMock.createMock(ProtoBufConfigManager.ProtoBufConfigFactory.class);
  }


  public void testInitializeWithValidConfig() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);
    ProgramConfiguration progConfig = ProgramConfiguration.newBuilder().build();
    ProtoBufConfig protoBufConfig = EasyMock.createMock(ProtoBufConfig.class);

    source.initialize();
    source.register(manager);
    EasyMock.expect(source.getConfigData()).andReturn(progConfig);
    EasyMock.expect(configFactory.create(progConfig)).andReturn(protoBufConfig);

    EasyMock.replay(source);
    EasyMock.replay(configFactory);
    EasyMock.replay(protoBufConfig);

    manager.initialize();
  }

  public void testInitializeWithSourceInitializeThrowingIOException() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);

    source.initialize();
    EasyMock.expectLastCall().andThrow(new IOException());

    EasyMock.replay(source);
    EasyMock.replay(configFactory);

    try {
      manager.initialize();
      fail("failed to propagate exception up");
    } catch (IOException expected) {
      // correct for this test
    }
  }

  public void testInitializeWithFactoryThrowInvalidConfigurationException() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);
    ProgramConfiguration progConfig = ProgramConfiguration.newBuilder().build();

    source.initialize();
    source.register(manager);
    EasyMock.expect(source.getConfigData()).andReturn(progConfig);
    EasyMock.expect(configFactory.create(progConfig))
        .andThrow(new InvalidConfigurationException());


    EasyMock.replay(source);
    EasyMock.replay(configFactory);

    try {
      manager.initialize();
      fail("failed to propagate exception up");
    } catch (InvalidConfigurationException expected) {
      // invalid configuration exceptions should be propagated up and not swallowed on
      // initialization only
    }
  }

  public void testShutdown() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);
    EasyMock.expect(source.deregister(manager)).andReturn(true);
    source.shutdown();

    EasyMock.replay(source);
    EasyMock.replay(configFactory);

    manager.shutdown();

    // shouldn't error if we call it again
    manager.shutdown();
  }

  public void testHandleProtoBufUpdateWithMultipleValidProtos() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);

    // make a few pairs of ProgramConfigurations and ProtoBufConfig so we can exercise the
    // update functionality
    ProgramConfiguration progConfig1 = ProgramConfiguration.newBuilder().build();
    ProtoBufConfig protoBufConfig1 = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration progConfig2 = ProgramConfiguration.newBuilder().build();
    ProtoBufConfig protoBufConfig2 = EasyMock.createMock(ProtoBufConfig.class);

    EasyMock.expect(configFactory.create(progConfig1)).andReturn(protoBufConfig1);
    EasyMock.expect(configFactory.create(progConfig2)).andReturn(protoBufConfig2);

    EasyMock.replay(source);
    EasyMock.replay(configFactory);
    EasyMock.replay(protoBufConfig1);
    EasyMock.replay(protoBufConfig2);


    manager.handleProtoBufUpdate(progConfig1);
    assertEquals("Initial update", protoBufConfig1, manager.queryConfig());
    manager.handleProtoBufUpdate(progConfig2);
    assertEquals("Second update", protoBufConfig2, manager.queryConfig());
  }

  public void testHandleProtoBufUpdateWithBothGoodAndBadProto() throws Exception {
    ProtoBufConfigManager manager = new ProtoBufConfigManager(source, configFactory);

    // make a few pairs of ProgramConfigurations and ProtoBufConfig so we can exercise the
    // update functionality
    ProgramConfiguration progConfig1 = ProgramConfiguration.newBuilder().build();
    ProtoBufConfig protoBufConfig1 = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration progConfig2 = ProgramConfiguration.newBuilder().build();
    ProgramConfiguration progConfig3 = ProgramConfiguration.newBuilder().build();
    ProtoBufConfig protoBufConfig3 = EasyMock.createMock(ProtoBufConfig.class);

    EasyMock.expect(configFactory.create(progConfig1)).andReturn(protoBufConfig1);
    EasyMock.expect(configFactory.create(progConfig2))
        .andThrow(new InvalidConfigurationException());
    EasyMock.expect(configFactory.create(progConfig3)).andReturn(protoBufConfig3);

    EasyMock.replay(source);
    EasyMock.replay(configFactory);
    EasyMock.replay(protoBufConfig1);
    EasyMock.replay(protoBufConfig3);


    manager.handleProtoBufUpdate(progConfig1);
    assertEquals("Initial update", protoBufConfig1, manager.queryConfig());
    // this should generate some noise in the logs
    manager.handleProtoBufUpdate(progConfig2);
    assertEquals("Second update", protoBufConfig1, manager.queryConfig());
    manager.handleProtoBufUpdate(progConfig3);
    assertEquals("Second update", protoBufConfig3, manager.queryConfig());
  }
}
