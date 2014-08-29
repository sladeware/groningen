/* Copyright 2013 Google, Inc.
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

package org.arbeitspferde.groningen.utility.logstream;

import com.google.inject.Singleton;
import com.google.protobuf.Message;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * {@link FileOutputLogStreamFactory} is responsible for provisioning {@link FileOutputLogStream}
 * instances.
 */
@Singleton
public class FileOutputLogStreamFactory implements OutputLogStreamFactory {

  private static final Logger log = Logger.getLogger(
      FileOutputLogStreamFactory.class.getCanonicalName());

  /**
   * This class is responsible for writing Protocol Buffer messages to a file.
   */
  @NotThreadSafe
  public class FileOutputLogStream implements OutputLogStream {
    FileOutputStream stream = null;

    public FileOutputLogStream(File file) throws IOException {
      if (!file.exists()) {
        file.createNewFile();
      }
      this.stream = new FileOutputStream(file);
    }

    @Override
    public void flush() throws IOException {
      stream.flush();
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }

    @Override
    public void write(final Message message) throws IOException {
      stream.write(message.toByteArray());
    }
  }

  @Override
  public OutputLogStream forStream(final OutputStream stream) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public OutputLogStream rotatingStreamForSpecification(final Specification spec)
      throws IOException {
    File file = new File(spec.getFilenamePrefix());
    log.info(String.format("Creating FileOutputLogStream log around %s.",
            spec.getFilenamePrefix()));
    return new FileOutputLogStream(file);
  }
}