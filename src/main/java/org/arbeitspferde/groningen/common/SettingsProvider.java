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

package org.arbeitspferde.groningen.common;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

@Singleton
public class SettingsProvider implements Provider<Settings> {
  private static final Logger log = Logger.getLogger(SettingsProvider.class.getCanonicalName());
  private static final String PROXIED_FLAGS_PACKAGE_WHITELIST_PREFIX = "org.arbeitspferde";

  private final String[] args;
  private final SupplementalSettingsProcessor supplementalSettingsProcessor;

  @Option(
      name = "--port",
      usage = "The port on which to service HTTP requests.")
  public Integer port = 80;

  @Option(
      name = "--startupSubservicesDeadlineSeconds",
      usage = "How long Groningen will wait at most for its subservices to start (seconds).")
  public Integer startupSubservicesDeadlineSeconds = 60;

  @Option(
      name = "--shutdownSubservicesDeadlineSeconds",
      usage = "How long Groningen will wait at most for its subservices to stop (seconds).")
  public Integer shutdownSubservicesDeadlineSeconds = 60;

  @Option(
      name = "--configFileNames",
      aliases = {"--f", "--configFileName" /* TOTO(mbushkov): depreacated, remove soon */},
      usage = "Comma-separated list of fully-qualified paths to the configuration files (one " +
          "per pipeline).")
  public String configFileNames;

  @Option(
      name = "--eventLogPrefix",
      usage = "The path along with base string prefix for the Groningen event log.")
  public String eventLogPrefix = "alloc/logs/tmp-groningen_events";

  @Option(
      name = "--eventLogRotateBytesSize",
      usage = "The quantity in bytes that the Groningen event log may grow before being rotated.")
  public Integer eventLogRotateSizeBytes = 524288000;

  @Option(
      name = "--eventLogFlushIntervalSeconds",
      usage = "The number of seconds that may transpire between Groningen event log flushing.")
  public Integer eventLogFlushIntervalSeconds = 60;

  @Inject
  public SettingsProvider(final String[] args,
      final SupplementalSettingsProcessor supplementalSettingsProcessor) {
    this.args = args;
    this.supplementalSettingsProcessor = supplementalSettingsProcessor;
  }

  @Override
  public Settings get() {
    log.info("Providing settings.");

    final Collection<Field> annotated = findAnnotatedFields();
    final Collection<String> args4jWhitelist = makeArgs4jWhitelist(annotated);
    final PartitionedArguments partitioned =
        separateArg4jArgumentsFromOthers(args, args4jWhitelist);
    final Collection<String> gnuSanitizedArg4sjArguments =
        filterGnuStyleArguments(partitioned.getArgs4jArguments());

    try {
      new CmdLineParser(this).parseArgument(gnuSanitizedArg4sjArguments);
    } catch (final CmdLineException e) {
      throw new ProvisionException("Error processing arg4j arguments.", e);
    }

    final Collection<String> unmatched = partitioned.getOtherArguments();
    final String[] matchedAsArray =
        gnuSanitizedArg4sjArguments.toArray(new String[gnuSanitizedArg4sjArguments.size()]);
    final String[] unmatchedAsArray = unmatched.toArray(new String[unmatched.size()]);
    supplementalSettingsProcessor.process(matchedAsArray, unmatchedAsArray);

    return new Settings() {

      @Override
      public Integer getPort() {
        return port;
      }

      @Override
      public Integer getStartupSubservicesDeadlineSeconds() {
        return startupSubservicesDeadlineSeconds;
      }

      @Override
      public Integer getShutdownSubservicesDeadlineSeconds() {
        return shutdownSubservicesDeadlineSeconds;
      }

      @Override
      public String[] getConfigFileNames() {
        return configFileNames.split(",");
      }

      @Override
      public String getEventLogPrefix() {
        return eventLogPrefix;
      }

      @Override
      public Integer getEventLogRotateSizeBytes() {
        return eventLogRotateSizeBytes;
      }

      @Override
      public Integer getEventLogFlushIntervalSeconds() {
        return eventLogFlushIntervalSeconds;
      }
    };
  }

  private Collection<Field> findAnnotatedFields() {
    final Reflections reflections = new Reflections(new ConfigurationBuilder()
        .filterInputsBy(new FilterBuilder()
            .include(FilterBuilder.prefix(PROXIED_FLAGS_PACKAGE_WHITELIST_PREFIX)))
        .setUrls(ClasspathHelper.forPackage(PROXIED_FLAGS_PACKAGE_WHITELIST_PREFIX))
        .setScanners(new FieldAnnotationsScanner()));

    return reflections.getFieldsAnnotatedWith(Option.class);
  }

  private Collection<String> makeArgs4jWhitelist(final Collection<Field> fields) {
    final Collection<String> emission = new HashSet<String>();

    for (final Field field : fields) {
      final Option optionAnnotation = field.getAnnotation(Option.class);

      if (optionAnnotation != null) {
        emission.add(optionAnnotation.name());
      }

      final String[] aliases = optionAnnotation.aliases();

      if (aliases != null) {
        for (final String alias : aliases) {
          emission.add(alias);
        }
      }
    }
    return emission;
  }

  private PartitionedArguments separateArg4jArgumentsFromOthers(final String[] arguments,
      final Collection<String> args4jPrefixWhitelist) {

    final ImmutableList.Builder<String> args4jArguments = ImmutableList.builder();
    final ImmutableList.Builder<String> unmatchedArguments = ImmutableList.builder();

    for (final String argument : args) {
      if (argument.startsWith("--")) {
        boolean wasAdded = false;
        for (final String args4jCandidate : args4jPrefixWhitelist) {
          if (!wasAdded && argument.startsWith(args4jCandidate)) {
            args4jArguments.add(argument);
            wasAdded = true;
          }
        }
        if (!wasAdded) {
          unmatchedArguments.add(argument);
        }
      }
    }

    return new PartitionedArguments() {

      @Override
      public ImmutableList<String> getOtherArguments() {
        return unmatchedArguments.build();
      }

      @Override
      public ImmutableList<String> getArgs4jArguments() {
        return args4jArguments.build();
      }
    };
  }

  private ImmutableList<String> filterGnuStyleArguments(final List<String> arguments) {
    final ImmutableList.Builder<String> gnuSanitizedArguments = ImmutableList.builder();

    for (int i = 0; i < arguments.size(); i++) {
      final String str = arguments.get(i);
      if (str.equals("--")) {
        while (i < arguments.size()) {
          gnuSanitizedArguments.add(arguments.get(i++));
        }
        break;
      }

      if (str.startsWith("--")) {
        final int eq = str.indexOf('=');
        if (eq > 0) {
          gnuSanitizedArguments.add(str.substring(0, eq));
          gnuSanitizedArguments.add(str.substring(eq + 1));
          continue;
        } else {
          gnuSanitizedArguments.add(str);
        }
      }

      gnuSanitizedArguments.add(str);
    }

    return gnuSanitizedArguments.build();
  }

  private interface PartitionedArguments {
    ImmutableList<String> getArgs4jArguments();
    ImmutableList<String> getOtherArguments();
  }
}
