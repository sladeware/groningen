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


import com.google.protobuf.Message;
import junit.framework.TestCase;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.easymock.EasyMock;

/**
 * Tests for {@link SafeProtoLogger}.
 */
public class SafeProtoLoggerTest extends TestCase {
  private SafeProtoLogger<Event.EventEntry> logger;
  private OutputLogStream mockOutputLogStream;
  private String loggerName;


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mockOutputLogStream = EasyMock.createMock(OutputLogStream.class);
    loggerName = "foo";

    logger = new SafeProtoLogger<Event.EventEntry>(mockOutputLogStream, loggerName);
  }

  public void testLogProtoEntry() throws Exception {
    mockOutputLogStream.write(EasyMock.isA(Message.class));
    EasyMock.expectLastCall();


    mockOutputLogStream.flush();
    EasyMock.expectLastCall();

    EasyMock.replay(mockOutputLogStream);

    final Event.EventEntry event = Event.EventEntry.newBuilder().build();
    logger.logProtoEntry(event);
    logger.flush();

    EasyMock.verify(mockOutputLogStream);
  }
}
