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

package org.arbeitspferde.groningen.utility;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple interface to encapsulate the file system access needs of Groningen such that any
 * underlying storage system can be used.
 *
 * It is presumed to not be thread-safe due the the intricacies of some of the various cluster
 * file storage systems interface libraries.
 */
@NotThreadSafe
public interface AbstractFile {
  public boolean delete() throws IOException, SecurityException;
  public boolean exists() throws IOException;
  public OutputStream outputStreamFor() throws IOException;
  public InputStream inputStreamFor() throws IOException;
  public void renameTo(final String newName) throws IOException;
}
