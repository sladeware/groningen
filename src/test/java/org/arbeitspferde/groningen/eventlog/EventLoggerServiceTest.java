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

package org.arbeitspferde.groningen.eventlog;


import junit.framework.TestCase;
import org.arbeitspferde.groningen.common.Settings;
import org.arbeitspferde.groningen.proto.Event;
import org.easymock.EasyMock;

/**
 * Tests for {@link EventLoggerService}.
 */
public class EventLoggerServiceTest extends TestCase {
  private SafeProtoLoggerFactory mockSafeProtoLoggerFactory;
  private SafeProtoLogger<Event.EventEntry> mockEventLogger;
  private Settings mockSettings;

  private EventLoggerService eventLoggerService;

  @Override
  protected void setUp() throws Exception {
    mockSafeProtoLoggerFactory = EasyMock.createMock(SafeProtoLoggerFactory.class);
    mockEventLogger = EasyMock.createMock(SafeProtoLogger.class);
    mockSettings = EasyMock.createMock(Settings.class);

    EasyMock.expect(mockSettings.getPort()).andReturn(80);
    EasyMock.expect(mockSettings.getEventLogRotateSizeBytes())
        .andReturn(524288000).atLeastOnce();
    EasyMock.expect(mockSettings.getEventLogFlushIntervalSeconds())
        .andReturn(60).atLeastOnce();
    EasyMock.expect(mockSettings.getEventLogPrefix())
        .andReturn("alloc/logs/tmp-groningen_events");
    EasyMock.replay(mockSettings);

    eventLoggerService = new EventLoggerService(mockSafeProtoLoggerFactory, mockSettings);
  }

  public void testGetLogger_Unprovisioned() {
    EasyMock.replay(mockSafeProtoLoggerFactory);
    EasyMock.replay(mockEventLogger);


    try {
      eventLoggerService.getLogger();
      fail("Should have emitted IllegalStateException for uninitialized logger.");
    } catch (final IllegalStateException expected) {
    }

    EasyMock.verify(mockSafeProtoLoggerFactory);
    EasyMock.verify(mockEventLogger);
  }

  public void testGetLogger_Provisioned() throws Exception {
    EasyMock.expect(
        mockSafeProtoLoggerFactory
            .newEventEntryLogger("alloc/logs/tmp-groningen_events", 80, 524288000, 60,
        "genericEventLogger")).andReturn(mockEventLogger);
    EasyMock.replay(mockSafeProtoLoggerFactory);
    EasyMock.replay(mockEventLogger);

    eventLoggerService.startUp();
    assertNotNull(eventLoggerService.getLogger());

    EasyMock.verify(mockSafeProtoLoggerFactory);
    EasyMock.verify(mockEventLogger);
  }

  public void testStartUp() throws Exception {
    EasyMock.expect(
        mockSafeProtoLoggerFactory
            .newEventEntryLogger("alloc/logs/tmp-groningen_events", 80, 524288000, 60,
        "genericEventLogger")).andReturn(mockEventLogger);

    EasyMock.replay(mockSafeProtoLoggerFactory);
    EasyMock.replay(mockEventLogger);

    eventLoggerService.startUp();

    EasyMock.verify(mockSafeProtoLoggerFactory);
    EasyMock.verify(mockEventLogger);
  }

  public void testShutdown_Unprovisioned() throws Exception {
    EasyMock.replay(mockSafeProtoLoggerFactory);
    EasyMock.replay(mockEventLogger);

    eventLoggerService.shutDown();

    EasyMock.verify(mockSafeProtoLoggerFactory);
    EasyMock.verify(mockEventLogger);
  }

  public void testShutdown_Provisioned() throws Exception {
    EasyMock.expect(
        mockSafeProtoLoggerFactory
            .newEventEntryLogger("alloc/logs/tmp-groningen_events", 80, 524288000, 60,
        "genericEventLogger")).andReturn(mockEventLogger);
    mockEventLogger.flush();
    EasyMock.expectLastCall();
    EasyMock.replay(mockSafeProtoLoggerFactory);
    EasyMock.replay(mockEventLogger);

    eventLoggerService.startUp();
    eventLoggerService.shutDown();

    EasyMock.verify(mockSafeProtoLoggerFactory);
    EasyMock.verify(mockEventLogger);
  }
}
