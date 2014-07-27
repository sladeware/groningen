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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.arbeitspferde.groningen.config.ProtoBufSource.ProtoBufSourceListener;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.utility.FileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Arbitrator between a backing protobuf based config source and the resulting config object.
 * This class will instantiate the requested config source, retrieve the protobuf based
 * configuration and produce a {@link GroningenConfig}, and act on any further notifications from
 * the backing source.
 *
 * <p>Currently does not support updating existing {@link ProtoBufConfig} instances (in fact
 * {@code ProtoBufConfig} prohibits this).
 *
 * <p>The choice of backend {@link ProtoBufSource} are chosen based on the {@code location} string
 * passed into the constructor, a string which is divided into ':' separated tokens. This class
 * reads the initial token and passes the rest of the string to the ProtoBufSource implementation.
 *
 * <p>Currently supported backends with their token are documented at
 * {@link #ProtoBufConfigManager(String)}.
 */
public class ProtoBufConfigManager implements ConfigManager, ProtoBufSourceListener {
  // Logger for this class
  private static final Logger logger = Logger.getLogger(ProtoBufConfigManager.class.getName());

  // Factory for creating ProtoBufConfig objects
  private final ProtoBufConfigFactory protoBufConfigFactory;

  // the backing source that produces configuration protocol buffers
  private ProtoBufSource protoSource = null;

  // the current GroningenConfig to be passed out of the config system
  private ProtoBufConfig protoConfig = null;

  /**
   * Construction using default factories.
   *
   * <p>Currently supported backends with their token:
   * <ul>
   * <li>ProtoBufFileSource: txtfile
   * <li>ProtoBufBinaryFileSource: binfile
   * </ul>
   *
   * <p>Does not do initialization as such a task is expected to have the
   * possibility of throwing non-runtime exceptions which in the design, we would like to
   * consolidate in one place - {@link #initialize()}, since {@code ProtoBufSource}
   * implementations should do the same.
   *
   * @param location string describing config backend
   */
  @Inject
  public ProtoBufConfigManager(@Assisted final String location, final FileFactory fileFactory,
     final FileEventNotifierFactory fileEventNotifierFactory,
     final LegacyProgramConfigurationMediator legacyProgramConfigurationMediator) {

    this(generateProtoBufSource(location, fileFactory, fileEventNotifierFactory,
        legacyProgramConfigurationMediator),
        new ProtoBufConfigFactory());
  }

  /**
   * General purpose constructor
   *
   * @param source the configuration protobuf generator to use with this instance
   * @param factory factory that generates {@code ProtoBufConfig} instances for a given
   * ProgramConfiguration
   */
  @VisibleForTesting
  ProtoBufConfigManager(ProtoBufSource source, ProtoBufConfigFactory factory) {
    this.protoSource = source;
    this.protoBufConfigFactory = factory;
  }

  @Override
  public synchronized void initialize() throws IOException, InvalidConfigurationException {
    protoSource.initialize();
    protoSource.register(this);
    ProgramConfiguration programConfig = protoSource.getConfigData();
    protoConfig = protoBufConfigFactory.create(programConfig);
  }

  @Override
  public void shutdown() throws IOException {
    if (protoSource != null) {
      protoSource.deregister(this);
      protoSource.shutdown();
      protoSource = null;
    }
  }

  @Override
  public GroningenConfig queryConfig() {
    return protoConfig;
  }

  @Override
  public synchronized void handleProtoBufUpdate(ProgramConfiguration configProto) {
    try {
      // since create() will throw if this configuration is invalid, we will never overwrite
      // the current config with an invalid one.
      protoConfig = protoBufConfigFactory.create(configProto);
    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.SEVERE, "New configuration was invalid, leaving old configuration in place", e);
    }
  }

  /**
   * Creates a configuration source via the first token of the location description passed in.
   * Initialization of the source is not done here. Tokens are ':' separated as noted in the class
   * header. Currently supported backends with expected token are documented at
   * {@link #ProtoBufConfigManager(String)}.
   *
   * @param sourceStr string describing the configuration source. The first token is stripped and
   *     used here, the remainder is passed to subsequent layers
   * @return the new {@link ProtoBufSource} object, ready to dispense config protos
   */
  @VisibleForTesting
  static ProtoBufSource generateProtoBufSource(final String sourceStr,
      final FileFactory fileFactory, final FileEventNotifierFactory fileEventNotifierFactory,
      final LegacyProgramConfigurationMediator legacyProgramConfigurationMediator) {

    // determine which underlying source type the user is asking for
    Preconditions.checkNotNull(sourceStr, "location cannot be null");
    String[] sourceStrParts = sourceStr.split(":", 2);
    Preconditions.checkArgument(
        sourceStrParts.length == 2, "could not parse prototype:type_params from source string");

    ProtoBufSource source = null;
    switch (sourceStrParts[0]) {
      case "binfile":
        source = new ProtoBufBinaryFileSource(sourceStrParts[1], fileFactory,
            fileEventNotifierFactory, legacyProgramConfigurationMediator);
        break;
      case "txtfile":
        source = new ProtoBufFileSource(sourceStrParts[1], fileFactory, fileEventNotifierFactory,
            legacyProgramConfigurationMediator);
        break;
      default:
        throw new IllegalArgumentException("unknown backend prototype: " + sourceStrParts[0]);
    }

    return source;
  }

  /**
   * Create a ProtoBufConfig object
   */
  @VisibleForTesting
  static class ProtoBufConfigFactory {
    public ProtoBufConfig create(ProgramConfiguration config)
        throws InvalidConfigurationException {
      return new ProtoBufConfig(config);
    }
  }
}
