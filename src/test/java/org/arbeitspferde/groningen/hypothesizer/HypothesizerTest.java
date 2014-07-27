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

package org.arbeitspferde.groningen.hypothesizer;


import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.utility.MetricExporter;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * The test for {@link Hypothesizer}.
 */
public class HypothesizerTest extends ClockedExperimentDbTestCaseBase {

  private static final int POPULATION_SIZE = 10;

  /** The object instance we are testing. */
  private Hypothesizer hypothesizer;
  private ExperimentDb experimentDb;
  private MetricExporter metricExporter;

  private IMocksControl mocksControl;

  @Override protected void setUp() throws Exception {
    super.setUp();

    mocksControl = EasyMock.createNiceControl();
    experimentDb = mocksControl.createMock(ExperimentDb.class);
    metricExporter = mocksControl.createMock(MetricExporter.class);

    hypothesizer = new Hypothesizer(clock, monitor, experimentDb, metricExporter);
  }

  /**
   * Tests {@link Hypothesizer#createInitialPopulation()} with invalid population size.
   */
  public void testCreateInitialPopulationWithInvalidPopulationSize() {
    // Return negative population size.
    ConfigManager cm = new StubConfigManager();
    GroningenConfig config = cm.queryConfig();
    try {
      hypothesizer.setPopulationSize(-1);
      hypothesizer.profiledRun(config);
      fail("Expected RuntimeException");
    } catch (RuntimeException expectedSoIgnore) {}

    // Return zero population size.
    try {
      hypothesizer.setPopulationSize(0);
      hypothesizer.profiledRun(config);
      fail("Expected RuntimeException");
    } catch (RuntimeException expectedSoIgnore) {}
  }

  /**
   * Tests {@link Hypothesizer#createInitialPopulation()} with invalid experiment ID.
   */
  public void testCreateInitialPopulationWithInvalidExperimentId() throws Exception {
    // Return negative population size.
    EasyMock.expect(experimentDb.nextExperimentId()).andReturn(-1L).once();
    mocksControl.replay();
    ConfigManager cm = new StubConfigManager();
    GroningenConfig config = cm.queryConfig();
    try {
      hypothesizer.setPopulationSize(POPULATION_SIZE);
      hypothesizer.profiledRun(config);
      fail("Expected RuntimeException");
    } catch (RuntimeException expectedSoIgnore) {}

    // Return zero population size.
    mocksControl.reset();
    EasyMock.expect(experimentDb.nextExperimentId()).andReturn(1L).once();
    mocksControl.replay();

    try {
      hypothesizer.setPopulationSize(0);
      hypothesizer.profiledRun(config);
      fail("Expected RuntimeException");
    } catch (RuntimeException expectedSoIgnore) {}
  }
}
