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

package org.arbeitspferde.groningen.utility.open;

import com.google.inject.Singleton;

import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.FileFactory;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Produce {@link AbstractFile}s that proxy {@link LocalFile} and
 * {@link AbstractFile} operations.
 */
@Singleton
public class LocalFileFactory implements FileFactory {
  private static final Logger log = Logger.getLogger(LocalFileFactory.class.getCanonicalName());

  @Override
  public AbstractFile forFile(String path, String mode) throws IOException {
    log.info(String.format("Creating LocalFile proxy for '%s'. Ignoring mode.", path));
    return new LocalFileProxy(path);
  }

  @Override
  public AbstractFile forFile(String path) throws IOException {
    log.info(String.format("Creating LocalFile proxy for '%s'.", path));
    return new LocalFileProxy(path);
  }

  @NotThreadSafe
  private class LocalFileProxy implements AbstractFile {
    private final File file;

    public LocalFileProxy(final String path) {
      this.file = new File(path);
    }

    @Override
    public boolean delete() throws IOException, SecurityException {
      return file.delete();
    }

    @Override
    public boolean exists() throws IOException {
      return file.exists();
    }

    @Override
    public OutputStream outputStreamFor() throws IOException {
      return new FileOutputStream(file);
    }

    @Override
    public InputStream inputStreamFor() throws IOException {
      return new FileInputStream(file);
    }

    @Override
    public void renameTo(String newName) throws IOException {
      file.renameTo(new File(newName));
    }

  }

}
