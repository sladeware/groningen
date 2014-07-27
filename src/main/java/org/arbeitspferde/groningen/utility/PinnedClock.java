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

package org.arbeitspferde.groningen.utility;

import com.google.common.base.Optional;

import org.joda.time.Instant;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This is merely a {@link Clock} that is useful for testing contexts.
 */
public class PinnedClock implements Clock {
  private static final Optional<Long> noMutation = Optional.of(0L);

  private final AtomicLong time;
  private final Optional<Long> incrementQueryBy;
  /**
   * @param time The time at which this clock shall report.
   */
  public PinnedClock(final long time) {
    this.time = new AtomicLong(time);
    incrementQueryBy = Optional.absent();
  }

  /**
   * @param time The initial time at which this clock shall report.
   * @param incrementQueryBy The amount by which the clock will be mutated on each query for time.
   */
  public PinnedClock(final long time, final long incrementQueryBy) {
    this.time = new AtomicLong(time);
    this.incrementQueryBy = Optional.of(incrementQueryBy);
  }

  @Override
  public Instant now() {
    try {
      return new Instant(time.get());
    } finally {
      time.addAndGet(incrementQueryBy.or(noMutation).get());
    }
  }
}
