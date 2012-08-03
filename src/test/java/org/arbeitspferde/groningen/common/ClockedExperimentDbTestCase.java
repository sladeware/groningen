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


import junit.framework.TestCase;

import org.arbeitspferde.groningen.LocalFileFactory;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.utility.MetricListener;
import org.arbeitspferde.groningen.utility.PinnedClock;
import org.arbeitspferde.groningen.utility.open.NullInputLogStreamFactory;
import org.arbeitspferde.groningen.utility.open.NullOutputLogStreamFactory;

/**
 * Test case base class for tests using {@link ExperimentDb} and {@link Clock}.
 */
public class ClockedExperimentDbTestCase extends TestCase {
  /** Contants used for mocked clock source */
  protected static final long DEFAULT_TIME_MS = 1000000000L;
  protected static final long INCREMENT_MS = 1234L;

  /** Mocked clock source */
  protected PinnedClock clock;

  /** A DisplayMediator for testing */
  protected MonitorGroningen monitor;

  /** The experimental database we're using for testing. */
  protected ExperimentDb experimentDb;

  protected MetricExporter metricExporter;

  @Override
  protected void setUp() throws Exception {
    clock = new PinnedClock(DEFAULT_TIME_MS, INCREMENT_MS);

    experimentDb = new ExperimentDb(clock, new LocalFileFactory(),
        new NullInputLogStreamFactory(), new NullOutputLogStreamFactory());
    monitor = new DisplayMediator(clock, experimentDb);
    metricExporter = new MetricExporter() {
      @Override
      public void register(String name, String description, MetricListener<?> metric) {}

      @Override
      public void init() {}
    };
  }
}
