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

import com.google.common.base.Preconditions;
import com.google.protobuf.GeneratedMessage;

import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;

/*
 [OutputLogStream] -> [SafeProtoLogger]
 */

/**
 * A generic RecordIO-based Protocol Buffer logger that supports automatically flushing and rotating
 * log buffers on given intervals and works around {@link org.arbeitspferde.groningen.utility.logstream.OutputLogStream}'s single-threaded
 * design.
 *
 * @param <T> The type of Protocol Buffer byte message that shall be encoded in each emission to
 *            the underlying file stream.
 */
@ThreadSafe
public class SafeProtoLogger<T extends GeneratedMessage> implements Flushable {
  private static final Logger log = Logger.getLogger(SafeProtoLogger.class.getCanonicalName());

  private final OutputLogStream stream;
  private final String loggerName;
  private final AtomicLong recordsPendingFlush = new AtomicLong();

  /**
   * Create a logger.
   *
   * @param stream The RecordIO writer.
   * @param loggerName The log name.
   * @param flushEventTimer The timer that manages the automatic flushing.
   */
  SafeProtoLogger(final OutputLogStream stream, final String loggerName) {
    Preconditions.checkNotNull(stream, "stream may not be null.");
    Preconditions.checkNotNull(loggerName, "loggerName may not be null.");

    this.stream = stream;
    this.loggerName = loggerName;
  }

  public void logProtoEntry(final T proto) throws IOException {
    Preconditions.checkNotNull(proto, "proto may not be null.");

    if (!proto.isInitialized()) {
      throw new IllegalArgumentException(
          String.format("Unable to log uninitialized entry '''%s'''", proto));
    }
      proto.writeDelimitedTo(null);
    final ByteBuffer buffer = ByteBuffer.wrap(proto.toByteArray());

    synchronized (stream) {
      stream.write(buffer);
      recordsPendingFlush.incrementAndGet();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    flush();
  }

  @Override
  public void flush() throws IOException {
    log.info(String.format("%s attempting to flush %s records.", loggerName, recordsPendingFlush));

    synchronized (stream) {
      stream.flush();
      recordsPendingFlush.set(0);
    }
  }
}
