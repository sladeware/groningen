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

package org.arbeitspferde.groningen.open;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.arbeitspferde.groningen.common.SupplementalSettingsProcessor;
import org.arbeitspferde.groningen.common.open.NullSupplementalSettingsProcessor;
import org.arbeitspferde.groningen.config.LegacyProgramConfigurationMediator;
import org.arbeitspferde.groningen.config.open.NullLegacyProgramConfigurationMediator;
import org.arbeitspferde.groningen.extractor.CollectionLogAddressor;
import org.arbeitspferde.groningen.extractor.open.NullCollectionLogAddressor;
import org.arbeitspferde.groningen.security.VendorSecurityManager;
import org.arbeitspferde.groningen.security.open.NullSecurityManager;
import org.arbeitspferde.groningen.subject.HealthQuerier;
import org.arbeitspferde.groningen.subject.ServingAddressGenerator;
import org.arbeitspferde.groningen.subject.SubjectInterrogator;
import org.arbeitspferde.groningen.subject.SubjectManipulator;
import org.arbeitspferde.groningen.subject.open.NullHealthQuerier;
import org.arbeitspferde.groningen.subject.open.NullServingAddressGenerator;
import org.arbeitspferde.groningen.subject.open.NullSubjectInterrogator;
import org.arbeitspferde.groningen.subject.open.NullSubjectManipulator;
import org.arbeitspferde.groningen.utility.FileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.FileFactory;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.utility.logstream.InputLogStreamFactory;
import org.arbeitspferde.groningen.utility.logstream.NullInputLogStreamFactory;
import org.arbeitspferde.groningen.utility.logstream.NullOutputLogStreamFactory;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;
import org.arbeitspferde.groningen.utility.open.LocalFileFactory;
import org.arbeitspferde.groningen.utility.open.NullFileEventNotifierFactory;
import org.arbeitspferde.groningen.utility.open.NullMetricExporter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Groningen Guice module for components that have open source versions.
 */
public class OpenModule extends AbstractModule {
  private static final Logger log = Logger.getLogger(OpenModule.class.getCanonicalName());

  @Override
  protected void configure() {
    log.info("Binding open injectables.");

    bind(MetricExporter.class).to(NullMetricExporter.class);
    bind(SupplementalSettingsProcessor.class).to(NullSupplementalSettingsProcessor.class);
    bind(SubjectInterrogator.class).to(NullSubjectInterrogator.class);
    bind(FileFactory.class).to(LocalFileFactory.class);
    bind(FileEventNotifierFactory.class).to(NullFileEventNotifierFactory.class);
    bind(InputLogStreamFactory.class).to(NullInputLogStreamFactory.class);
    bind(OutputLogStreamFactory.class).to(NullOutputLogStreamFactory.class);
    bind(HealthQuerier.class).to(NullHealthQuerier.class);
    bind(SubjectManipulator.class).to(NullSubjectManipulator.class);
    bind(VendorSecurityManager.class).to(NullSecurityManager.class);
    bind(ServingAddressGenerator.class).to(NullServingAddressGenerator.class);
    bind(LegacyProgramConfigurationMediator.class)
        .to(NullLegacyProgramConfigurationMediator.class);
    bind(CollectionLogAddressor.class).to(NullCollectionLogAddressor.class);
  }

  /*
   * This is merely a short-term hack, as it will require inclusion of the <em>appropriate</em>
   * serving port.
   */
  @Provides
  @Singleton
  @Named("servingAddress")
  public String produceServingAddress() {
    try {
      final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        final NetworkInterface currentInterface = interfaces.nextElement();

        if (currentInterface.isLoopback()) {
          continue;
        }

        final Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();

        while (addresses.hasMoreElements()) {
          final InetAddress address = addresses.nextElement();
          final String canonicalName = address.getCanonicalHostName();

          if (!canonicalName.equalsIgnoreCase(address.getHostAddress())) {
            return canonicalName;
          }
        }
      }
    } catch (final SocketException e) {
      log.log(Level.SEVERE, "Error acquiring hostname due to networking stack problems.", e);
    }

    log.severe("Could not acquire hostname due to unknown reasons.");

    return "localhost";
  }

  @Provides
  @Singleton
  public HashFunction getHashFunction() {
    return Hashing.md5();
  }
}
