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

import com.google.inject.Provider;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.arbeitspferde.groningen.Helper;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;
import org.arbeitspferde.groningen.utility.logstream.format.open.DelimitedFactory;
import org.easymock.EasyMock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Tests for {@link SafeProtoLoggerFactory}.
 */
public class SafeProtoLoggerFactoryTest extends TestCase {
  private static final Logger log = Logger.getLogger(SafeProtoLoggerTest.class.getCanonicalName());

  private DelimitedFactory delimitedFactory;
  private SafeProtoLoggerFactory factory;
  private Provider<Timer> mockDaemonTimerProvider;
  private Timer mockTimer;
  private OutputLogStreamFactory fakeOutputLogStreamFactory;
  private File temporaryDirectory;
  private File temporaryLogFile;

  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    temporaryDirectory = Helper.getTestDirectory();
    temporaryLogFile = File.createTempFile("log", "", temporaryDirectory);

    mockDaemonTimerProvider = EasyMock.createMock(Provider.class);
    mockTimer = EasyMock.createMock(Timer.class);

    delimitedFactory = new DelimitedFactory();
    fakeOutputLogStreamFactory = new OutputLogStreamFactory() {

      @Override
      public OutputLogStream forStream(final OutputStream stream) throws IOException {
        return delimitedFactory.forStream(new FileOutputStream(temporaryLogFile));
      }

      @Override
      public OutputLogStream rotatingStreamForSpecification(Specification specification)
          throws IOException {
        try {
          return forStream(new FileOutputStream(temporaryLogFile));
        } catch (final FileNotFoundException e) {
          throw new IOException(e);
        }
      }
    };

    factory = new SafeProtoLoggerFactory(mockDaemonTimerProvider, fakeOutputLogStreamFactory);
  }

  @Override
  protected void tearDown() throws Exception {
     super.tearDown();

     FileUtils.deleteDirectory(temporaryDirectory);
  }

  public void testNewEventEntryLogger() throws Exception {
    EasyMock.expect(mockDaemonTimerProvider.get()).andReturn(mockTimer);
    mockTimer.schedule(EasyMock.isA(TimerTask.class), EasyMock.eq(0L), EasyMock.eq(5000L));
    EasyMock.expectLastCall();

    EasyMock.replay(mockTimer);
    EasyMock.replay(mockDaemonTimerProvider);

    final String testDirectory = temporaryDirectory.getAbsolutePath();
    final SafeProtoLogger<Event.EventEntry> logger =
        factory.newEventEntryLogger(testDirectory, 80, 100, 5, "bar");

    final Event.EventEntry.Builder eventBuilder = Event.EventEntry.newBuilder();

    eventBuilder.setExperimentId(1)
        .setTime(1000)
        .setGroningenUser("foo")
        .setGroningenServingAddress("localhost:25000")
        .setSubjectUser("bar")
        .setSubjectServingAddress("remotehost:25000")
        .setType(Event.EventEntry.Type.START);

    Event.EventEntry.JvmFlag jvmFlag = Event.EventEntry.JvmFlag.newBuilder()
      .setName("-Xmx")
      .setValue("1000")
      .setManagedByGroningen(true)
      .build();
    eventBuilder.addJvmFlag(jvmFlag);

    Event.EventEntry.FitnessScore fitnessScore = Event.EventEntry.FitnessScore.newBuilder()
      .setName("pause_time")
      .setScore(1000)
      .setCoefficient(1)
      .build();
    eventBuilder.addScore(fitnessScore);

    logger.logProtoEntry(eventBuilder.build());

    logger.flush();

    assertNotNull(logger);

    EasyMock.verify(mockTimer);
    EasyMock.verify(mockDaemonTimerProvider);
  }

  public void testYieldTimer_FlushesAutomatically() throws IOException, InterruptedException {
    final SafeProtoLogger<?> mockLogger = EasyMock.createMock(SafeProtoLogger.class);

    EasyMock.expect(mockDaemonTimerProvider.get()).andReturn(new Timer(true));
    mockLogger.flush();
    EasyMock.expectLastCall().atLeastOnce();

    EasyMock.replay(mockLogger);
    EasyMock.replay(mockTimer);
    EasyMock.replay(mockDaemonTimerProvider);

    factory.yieldTimer(1, "foo", mockLogger);

    // N.B. — Non-Deterministic.
    Thread.sleep(60);

    EasyMock.verify(mockLogger);
    EasyMock.verify(mockTimer);
    EasyMock.verify(mockDaemonTimerProvider);
  }
}
