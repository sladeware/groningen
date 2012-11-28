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

package org.arbeitspferde.groningen.utility.logstream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputLogStreamFactory} is responsible for provisioning {@link OutputLogStream} instances
 * for the user subject to the underlying storage and encoding methodology of the system at play.
 */
public interface OutputLogStreamFactory {
  /**
   * Create a {@link OutputLogStream} from a given stream.
   * @param stream The stream used for the storage.
   * @return The {@link OutputLogStream} used to encode the user data in the underlying format.
   * @throws IOException In case of an anomaly with the storage stack.
   */
  public OutputLogStream forStream(final OutputStream stream) throws IOException;

  /**
   * This is similar to {@link OutputLogStreamFactory#forStream} except that it creates a new
   * target emission file from the {@link Specification} subject to a given log rotation
   * specification.
   *
   * @param specification The specification used to dictate where and when log files are created
   *                      and rotated.
   * @return The {@link OutputLogStream} used to encode the user data in the underlying format,
   *         subject to a given rotation policy.
   * @throws IOException In case of an anomaly with the storage stack.
   */
  public OutputLogStream rotatingStreamForSpecification(final Specification specification)
      throws IOException;

  /**
   * The storage and log rotation storage policy
   */
  public static interface Specification {
    /**
     * The all members of the path leading up to the name should exist.
     *
     * @return The prefix for the emitted file's name.
     */
    public String getFilenamePrefix();

    /**
     * @return The port on which this given server is serving in case the underlying host
     *         multi-homes other things.
     */
    public int getServingPort();

    /**
     * Logs may grow large; rotate them automatically.
     *
     * @return The size before which the log file will be automatically rotated.
     */
    public long rotateFileUponAtSizeBytes();
  }
}
