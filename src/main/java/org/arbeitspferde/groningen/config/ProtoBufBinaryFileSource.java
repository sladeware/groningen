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
import org.arbeitspferde.groningen.utility.FileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Construct configuration protocol buffers from a given file containing a binary protocol
 * buffer and potentially monitor that file for updates by extending ProtoBufFileSource.
 */
public class ProtoBufBinaryFileSource extends ProtoBufFileSource {

  public ProtoBufBinaryFileSource(final String location, final FileFactory fileFactory,
      final FileEventNotifierFactory fileEventNotifierFactory,
      final LegacyProgramConfigurationMediator legacyProgramConfigurationMediator) {
    super(location, fileFactory, fileEventNotifierFactory, legacyProgramConfigurationMediator);
  }

  /** Slurps in the binary config from the provided inputstream. */
  @Override
  public void reloadProtoFromStream(InputStream inputStream) throws IOException {
    proto = ProgramConfiguration.parseFrom(inputStream);
  }
}
