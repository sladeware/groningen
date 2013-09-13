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

import org.arbeitspferde.groningen.HistoryDatastore;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineManager;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.scorer.HistoricalBestPerformerScorer;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.utility.MetricListener;
import org.arbeitspferde.groningen.utility.PinnedClock;
import org.easymock.EasyMock;

/**
 * Test case base class for tests using {@link ExperimentDb}.
 */
//TODO(mbushkov) renaming needed - Clock is no longer used.
public class ClockedExperimentDbTestCaseBase extends TestCase {
  /** Constants used for mocked clock source */
  protected static final long DEFAULT_TIME_MS = 1000000000L;
  protected static final long INCREMENT_MS = 1234L;

  /** Mocked clock source */
  protected PinnedClock clock;

  /** A DisplayMediator for testing */
  protected MonitorGroningen monitor;

  /** The experimental database we're using for testing. */
  protected ExperimentDb experimentDb;

  protected MetricExporter metricExporter;
  
  protected HistoryDatastore historyDataStoreMock;
  protected PipelineManager pipelineManagerMock;
  protected HistoricalBestPerformerScorer mockBestPerformerScorer;

  @Override
  protected void setUp() throws Exception {
    clock = new PinnedClock(DEFAULT_TIME_MS, INCREMENT_MS);

    historyDataStoreMock = EasyMock.createNiceMock(HistoryDatastore.class);
    pipelineManagerMock = EasyMock.createNiceMock(PipelineManager.class);
    mockBestPerformerScorer = EasyMock.createNiceMock(HistoricalBestPerformerScorer.class);

    experimentDb = new ExperimentDb();
    monitor = new DisplayMediator(experimentDb, historyDataStoreMock, pipelineManagerMock,
        new PipelineId("pipeline_id"), mockBestPerformerScorer);
    metricExporter = new MetricExporter() {
      @Override
      public void register(String name, String description, MetricListener<?> metric) {}

      @Override
      public void init() {}
    };
  }
}
