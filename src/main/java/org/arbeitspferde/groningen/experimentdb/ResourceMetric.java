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

/**
 * ResourceMetric caches in memory system resource utilization metrics as they
 * pass through the {@link ExperimentDb} on their way to Megastore when a GC
 * log is parsed by the {@link Extractor}. A subject's subjectId (see the
 * Megastore schema) can be used to lookup a ResourceMetric object in a
 * {@link Map}. Then that ResourceMetric object can be used to compute a score
 * by calling computeScore(). The computed score is based on the resource
 * requirements of the subject as it ran. It is useful to the {@link Hypothesizer}
 * when it creates a new set of experiments.
 */
public class ResourceMetric extends BaseComputeScore {

  /** Use this to choose the typoe of score you'd like to compute */
  public enum ScoreType {MEMORY};

  /** Creation by package only */
  ResourceMetric() { }

  /** The Java JVM memory footprint for this subject */
  private double memoryFootprint;

  /** Set the memoryFootprint of a subject */
  public void setMemoryFootprint(long memoryFootprint) {
    this.memoryFootprint = memoryFootprint;
  }

  protected double computeScoreImpl(Enum scoreType) {
    switch ((ScoreType) scoreType) {
      case MEMORY:
        return computeMemoryScore();
      default:
        return handleInvalidScoreType();
    }
  }

  private double computeMemoryScore() {
    double result = this.memoryFootprint;

    if (result > 0.0) {
      return 1.0 / result;
    } else {
      return 0.0;
    }
  }
}
