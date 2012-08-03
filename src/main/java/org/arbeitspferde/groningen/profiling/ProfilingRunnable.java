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

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.utility.Clock;
import org.joda.time.Instant;

import java.util.List;

/**
 * ProfilingRunnable gathers profiling data for the objects that extend it.
 * You need to implement your pipeline stage as profiledRun() to use it.
 */
public abstract class ProfilingRunnable {
  /** The profiled runs are stored here */
  private final List<Profile> profiles = Lists.newArrayList();

  /** We use a Clock object for time keeping, which can be easily mocked. */
  protected final Clock clock;

  /** We use a monitor to store and interface with HUD */
  protected final MonitorGroningen monitor;

  /**
   * Start up this {@link ProfilingRunnable} for use.
   */
  public abstract void startUp();

  /** Implement this method to profile a stage running within the pipeline. */
  public abstract void profiledRun(GroningenConfig config);


  public ProfilingRunnable(Clock clock, MonitorGroningen monitor) {
    this.clock = clock;
    this.monitor = monitor;
  }

  /** Returns a list of {@link Profile} objects collected so far. */
  public List<Profile> getProfiles() {
    return profiles;
  }

  /**
   * This is the general entry point of objects that use profiling. It allows
   * us to measure the elapsed time.
   */
  public void run(GroningenConfig config) {
    Instant start = clock.now();
    try {
      profiledRun(config);
    } finally {
      Instant end = clock.now();
      profiles.add(new Profile(start, end));
    }
  }
}
