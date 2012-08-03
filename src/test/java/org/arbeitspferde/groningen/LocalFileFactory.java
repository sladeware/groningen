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
import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple implementation of {@FileFactory} that is only useful for testing contexts.
 */
public class LocalFileFactory implements FileFactory {
    @Override
    public AbstractFile forFile(final String path, final String mode) {
      final File file = new File(path);

      return new AbstractFile() {
        @Override
        public boolean delete() throws SecurityException {
          return file.delete();
        }

        @Override
        public boolean exists() {
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
        public void renameTo(final String newName) throws IOException {
          final File destination = new File(newName);

          if (!file.renameTo(destination)) {
            throw new IOException("Could not rename file.");
          }
        }
      };
    }

    @Override
    public AbstractFile forFile(final String path) {
      return forFile(path, "r");
    }
  }
