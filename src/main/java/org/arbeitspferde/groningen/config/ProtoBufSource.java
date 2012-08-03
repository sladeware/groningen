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

package org.arbeitspferde.groningen.config;

import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;

import java.io.IOException;

/**
 * A conduit for conveying configuration protocol buffers from their source to your
 * listener. Provides methods for slurping in the protobuf, potentially watching for updates,
 * and passing the protobuf (and potentially updated versions of it) to any registered observers.
 */
public interface ProtoBufSource {

  /**
   * Interface by which observers will accept notification for updates.
   */
  interface ProtoBufSourceListener {
    /**
     * Handle the updated config text.
     *
     * @param configProto updated configuration information
     */
    public void handleProtoBufUpdate(final ProgramConfiguration configProto);
  }

  /**
   * Initialize the {@code ProtoBufSource} subsystem. This is present to separate exception
   * generating code from the construction of the object, to better allow for error handling, and
   * for consistent calling semantics across the different implementations, implementation which
   * are expected to be instantiated via a factory and then operated on as a {@code ProtoBufSource}.
   * Hence, the desire to keep exception throwing consistent between implementations.
   *
   * @throws IOException if an error occurs during initialization
   */
  public void initialize() throws IOException;

  /**
   * Shutdown the ProtoBufSource subsystem.
   *
   * @throws IOException exception types and reasons for throwing will vary by implementation
   */
  public void shutdown() throws IOException;

  /**
   * Register the ProtoBufConfig. Registering will not cause an initial update to be passed to the
   * registrant, only updates corresponding to changes in the underlying source will be pushed.
   *
   * @param protoConfigListener the object which should be sent updated protobufs when the config
   *     information is changed
   */
  public void register(final ProtoBufSourceListener protoConfigListener);

  /**
   * Deregister an observer.
   *
   * @return true iff the object was currently registered
   */
  public boolean deregister(final ProtoBufSourceListener protoConfig);

  /**
   * Access the current protobuf representation of the configuration source.
   *
   * @return the current protobuf representation of the configuration source
   */
  public ProgramConfiguration getConfigData();
}
