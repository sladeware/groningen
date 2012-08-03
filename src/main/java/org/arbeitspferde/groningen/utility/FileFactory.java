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

import java.io.IOException;

/**
 * A helper to build concrete {@link AbstractFile} instances that abstract away the underlying
 * storage mechanics of the cluster environment.
 */
public interface FileFactory {
  /**
   * Return a {@link AbstractFile} for the given path.
   *
   * @param path The path to interact with.
   * @param mode If {@code !mode.present()}, the file is opened in write mode.
   * @return The {@link AbstractFile} for this path.
   * @throws IOException If an anomaly occurs in the underlying storage stack.
   */
  public AbstractFile forFile(final String path, final String mode) throws IOException;

  /**
   * This has the same semantics as {@link #forFile(String, String)}, except that it has an
   * implied access {@link mode} of "r".
   */
  public AbstractFile forFile(final String path) throws IOException;
}
