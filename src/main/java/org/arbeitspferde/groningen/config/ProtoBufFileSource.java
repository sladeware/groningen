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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.TextFormat;

import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.utility.FileEventNotifier;
import org.arbeitspferde.groningen.utility.FileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Construct configuration protocol buffers from a given file and potentially monitor that
 * file for updates. Should an update occur, reload the protocol buffer and notify observers
 * of the update. Observers are left to request the new protocol buffer at their leisure.
 */
public class ProtoBufFileSource implements ProtoBufSource {
  // Logger for this class
  private static final Logger logger = Logger.getLogger(ProtoBufFileSource.class.getName());

  // Observers for notifications of updates to the configuration
  private final List<ProtoBufSourceListener> observers = Lists.newArrayList();

  // Path to the file which should be used as the text representation of the protobuf
  private String inputPath = null;

  // Polling period for updates to the above file
  private long refreshPeriod = 0L;

  // Notifier that polls the file for updates and executes a callback when one occurs
  protected FileEventNotifier notifier = null;

  // The current proto
  public ProgramConfiguration proto = null;

  // Lock protecting reloading of the source and pushing it into the protobuf such
  // that multiple updates to the file cannot result in an old version overwriting
  // a new one.
  private final Object reloadLock = new Object();

  private final FileFactory fileFactory;
  private final FileEventNotifierFactory fileEventNotifierFactory;
  private final LegacyProgramConfigurationMediator legacyProgramConfigurationMediator;

  /**
   * Construct a {@code ProtoBufFileSource}.
   *
   * @param location the path to the config file with format
   *     {@literal [(<flags>}:)*]<path>} where {@literal <flags>} can be:
   *     <ul>
   *     <li>refresh=\d+ sets file polling period in seconds.
   *     </ul>
   * @throws IllegalArgumentException if the location string cannot be parsed into flags and a
   *      path
   */
  public ProtoBufFileSource(final String location, final FileFactory fileFactory,
      final FileEventNotifierFactory fileEventNotifierFactory,
      final LegacyProgramConfigurationMediator legacyProgramConfigurationMediator) {
    this.fileFactory = fileFactory;
    this.fileEventNotifierFactory = fileEventNotifierFactory;
    this.legacyProgramConfigurationMediator = legacyProgramConfigurationMediator;

    Preconditions.checkNotNull(location, "location cannot be null");
    String [] locationParts = location.split(":");

    // parse out any flags included in the location, marking the last flag found
    int lastFlag = -1;
    for (int i = 0; i < locationParts.length; i++) {
      if (locationParts[i].matches("^refresh=\\d+$")) {
        lastFlag = i;
        refreshPeriod = Integer.parseInt(locationParts[i].substring(8));
      }
    }

    // verify we had a path
    inputPath = locationParts[locationParts.length - 1];
    if (inputPath == null || lastFlag == (locationParts.length - 1)) {
      throw new IllegalArgumentException("no input path specified");
    }
  }

  /**
   * {@inheritDoc}
   *
   * On return, a protobuf will have been loaded guaranteeing a non-null return for
   * {@code getConfigData()}
   */
  @Override
  public void initialize() throws IOException {

    /*
     * Setup the notifier before we grab the protobuf for the first time to make sure we don't
     * miss a revision of the file. Do the explicit reload instead of allowing the notifier
     * to trigger a reload on instantiation so we verify we have a valid path.
     */
    Runnable wrapReloadProtoFromFile = new Runnable() {
      @Override
      public void run() {
        try {
          reloadProtoFromFile();
        } catch (IOException e) {
          logger.log(Level.SEVERE,
              String.format("failed to reload file %s after changed noticed", inputPath), e);
        }
      }
    };

    if (refreshPeriod > 0) {
      logger.log(Level.FINE, "instantiating file notifier for file: %s", inputPath);
      notifier = fileEventNotifierFactory.fileEventNotifierFor(inputPath, refreshPeriod,
          TimeUnit.SECONDS, wrapReloadProtoFromFile, false);
    }

    reloadProtoFromFile();
  }

  @Override
  public void shutdown() {
    if (notifier != null) {
      notifier.stop();
      notifier = null;
    }
  }

  @Override
  public void register(ProtoBufSourceListener protoConfigurer) {
    synchronized (observers) {
      if (!observers.contains(protoConfigurer)) {
        observers.add(protoConfigurer);
      }
    }
  }

  @Override
  public boolean deregister(ProtoBufSourceListener protoConfigurer) {
    synchronized (observers) {
      return observers.remove(protoConfigurer);
    }
  }

  /**
   * Return the cached version of the current protobuf.
   *
   * {@inheritDoc}
   */
  @Override
  public ProgramConfiguration getConfigData() {
    return proto;
  }

  public String getInputPath() {
    return inputPath;
  }

  public void setInputPath(String path) {
    inputPath = path;
  }

  public long getRefreshPeriod() {
    return refreshPeriod;
  }

  public void setRefreshPeriod(long period) {
    refreshPeriod = period;
  }

  /**
   * Read in the contents of a file and produce a protobuf from the contents.
   *
   * Wraps reloading the proto from a given file, but farms out the actual acting on the
   * contents of the Readable to a method we can test without hitting the file system.
   */
  public void reloadProtoFromFile() throws IOException {
    logger.log(Level.FINE, "attempting to load proto from file: %s", inputPath);
    final InputStream inputStream = fileFactory.forFile(inputPath).inputStreamFor();

    // protect against multiple update race condition
    try {
      synchronized (reloadLock) {
        reloadProtoFromStream(inputStream);
        notifyObservers();
      }
    } finally {
      // we want to verify that we close the file as well.
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          logger.log(Level.SEVERE,
              String.format("failed to close input config file %s", inputPath), e);
        }
      }
    }
  }

  /**
   * Take an InputStream and load the protobuf from with in it. This method is
   * marked protected as it is expected to be overriden in subclasses that operate on
   * non text format protobufs.
   *
   * @param inputStream the source from which to read the file contents
   */
  public void reloadProtoFromStream(InputStream inputStream) throws IOException {
    final Reader reader = new InputStreamReader(inputStream);
    try {
      reloadProtoFromReadable(reader);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /** Slurps in the config from the readable and notifies registered observers of the update */
  public void reloadProtoFromReadable(Readable inputReader) throws IOException {
    // We basically hand the Readable with the text representation off to something that
    // knows how to parse it and stuff it in the Builder
    ProgramConfiguration.Builder configBuilder = ProgramConfiguration.newBuilder();

    final StringBuilder intermediateString = new StringBuilder();
    final CharBuffer buffer = CharBuffer.allocate(256);

    while (true) {
      final int length = inputReader.read(buffer);

      if (length == -1) {
        break;
      }

      buffer.flip();
      intermediateString.append(buffer, 0, length);
    }

    // TODO(team): rework such that this is a Reader that does rewrite on
    // the fly
    final String migratedString = legacyProgramConfigurationMediator
        .migrateLegacyConfiguration(intermediateString.toString());

    TextFormat.merge(migratedString, configBuilder);

    proto = configBuilder.build();
  }

  /** Notify all registered observers of the change */
  public void notifyObservers() {
    synchronized (observers) {
      // hold a local copy in case another thread generates another proto part while we
      // are still notifying for this one. That thread will block while we finish notification
      // and then renotify...
      ProgramConfiguration localProto = proto;
      for (ProtoBufSourceListener observer : observers) {
        observer.handleProtoBufUpdate(localProto);
      }
    }
  }
}
