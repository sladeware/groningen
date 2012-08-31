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

package org.arbeitspferde.groningen.profiling;


import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.profiling.Profile;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.utility.Clock;

/**
 * The test for {@link ProfilingRunnable}.
 */
public class ProfilingRunnableTest extends ClockedExperimentDbTestCaseBase {
  /** The test pipeline stage we are profiling */
  private TestPipelineStage testPipelineState = new TestPipelineStage(clock, monitor);

  public void testProfiledRun() {
    ConfigManager cm = new StubConfigManager();
    GroningenConfig config = cm.queryConfig();
    testPipelineState.profiledRun(config);

    for (Profile profile : testPipelineState.getProfiles()) {
      assertEquals(profile.getStart(), DEFAULT_TIME_MS);
      assertEquals(profile.getEnd(), DEFAULT_TIME_MS + INCREMENT_MS);
    }
  }

  static class TestPipelineStage extends ProfilingRunnable {
    public TestPipelineStage(Clock clock, MonitorGroningen displayMediator) {
      super(clock, displayMediator);
    }

    @Override
    public void profiledRun(GroningenConfig config) {
    }

    @Override
    public void startUp() {
    }
  }
}
