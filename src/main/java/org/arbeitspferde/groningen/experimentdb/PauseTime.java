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

package org.arbeitspferde.groningen.experimentdb;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.arbeitspferde.groningen.common.Statistics;

import java.util.List;
import java.util.Vector;

/**
 * PauseTime caches in memory GC log data as it passes through the
 * {@link ExperimentDb} when a GC log is parsed by the
 * {@link Extractor}. A subject's index can be used to lookup a PauseTime object
 * in a {@link Map}. Then that PauseTime object can be used to compute a score
 * by calling computeScore(). The computed score is based on the GC pauses
 * experienced by the subject as it ran. It is useful to the {@link Hypothesizer}
 * when it creates a new set of experiments.
 */
public class PauseTime extends BaseComputeScore {
  // This number is partially informed by values derived from examining the JTA logs.
  private static final int EXPECTED_NUMBER_OF_PAUSETIME_EVENTS_PER_SUBJECT_PER_EXPERIMENT = 100000;

  /** Use this to choose the typoe of score you'd like to compute */
  public enum ScoreType {LATENCY, THROUGHPUT};

  /** The list of pause time values for this subject */
  private final List<Double> pauseTimeDurations =
      new Vector<Double>(EXPECTED_NUMBER_OF_PAUSETIME_EVENTS_PER_SUBJECT_PER_EXPERIMENT);

  /** The total Java GC pause time for this subject */
  private double pauseTimeDurationTotal;

  /** The latency percentile */
  @VisibleForTesting double percentile;

  /** Creation by package only */
  PauseTime(final double percentile) {
    this.percentile = percentile;
  }

  PauseTime() {
    this(99);
  }


  /** Add the input pause time to the current pause time and store it */
  public void incrementPauseTime(final double pauseTime) {
    this.pauseTimeDurations.add(pauseTime);

    this.pauseTimeDurationTotal += pauseTime;
  }

  protected double computeScoreImpl(final Enum scoreType) {
    switch ((ScoreType) scoreType) {
      case LATENCY:
        return computeLatencyScore();
      case THROUGHPUT:
        return computeThroughputScore();
      default:
        return handleInvalidScoreType();
    }
  }

  /** Returns the aggregated pause times for this subject */
  public double getPauseTimeTotal() {
    return pauseTimeDurationTotal;
  }

  /** Set the throughput percentile for this subject */
  public void setPercentile(final double percentile) {
    this.percentile = percentile;
  }

  private double computeLatencyScore() {
    final double result = Statistics.computePercentile(getPauseDurations(), this.percentile);
    if (result > 0.0) {
      return 1.0 / result;
    } else {
      return 0.0;
    }
  }

  private double computeThroughputScore() {
    if (pauseTimeDurationTotal > 0.0) {
      return 1.0 / pauseTimeDurationTotal;
    } else {
      return 0.0;
    }
  }

  public List<Double> getPauseDurations() {
    return Lists.newArrayList(pauseTimeDurations);
  }
}
