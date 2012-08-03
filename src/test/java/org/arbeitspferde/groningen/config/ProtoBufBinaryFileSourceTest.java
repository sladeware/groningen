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


import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.arbeitspferde.groningen.Helper;
import org.arbeitspferde.groningen.LocalFileFactory;
import org.arbeitspferde.groningen.NullFileEventNotifierFactory;
import org.arbeitspferde.groningen.config.ProtoBufBinaryFileSource;
import org.arbeitspferde.groningen.config.open.NullLegacyProgramConfigurationMediator;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.ClusterConfig;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.ClusterConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.utility.FileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Tests for ProtoBufBinaryFileSource specific code.
 */
public class ProtoBufBinaryFileSourceTest extends TestCase {

  /**
   * Test to exercise loading a binary format protobuf. This requires accessing a file and
   * since there isn't a handy way to make a binary protobuf, just make it fresh each time.
   */
  public void testLoad() throws Exception {
    ProgramConfiguration config = ProgramConfiguration.newBuilder()
        .setUser("bigtester")
        .addCluster(ClusterConfig.newBuilder()
            .setCluster("xx")
            .addSubjectGroup(SubjectGroupConfig.newBuilder().
                setSubjectGroupName("bigTestingGroup").
                setExpSettingsFilesDir("/some/path").
                build())
            .build())
        .build();
    File tmpDir = Helper.getTestDirectory();

    try {
      String filename = tmpDir.getAbsolutePath() + "proto-bin.cfg";
      OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
      config.writeTo(out);
      out.close();

      final FileFactory fakeFileFactory = new LocalFileFactory();
      final FileEventNotifierFactory fakeFileEventNotifier = new NullFileEventNotifierFactory();

      ProtoBufBinaryFileSource source = new ProtoBufBinaryFileSource(filename, fakeFileFactory,
          fakeFileEventNotifier, new NullLegacyProgramConfigurationMediator());

      source.initialize();
      assertEquals(config, source.getConfigData());
    } finally {
      FileUtils.deleteDirectory(tmpDir);
    }
  }
}
