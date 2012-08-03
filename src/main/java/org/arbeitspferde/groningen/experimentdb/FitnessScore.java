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


import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;

import java.util.logging.Logger;

// TODO(team): Provide an elegant way of exposing the fitness score metrics via the new
//             information servlet.
/**
 * Computes the fitness score for an experiment.
 */
public class FitnessScore {
  /** Logger for this class */
  private static final Logger log = Logger.getLogger(FitnessScore.class.getCanonicalName());

  /**
   * Returns the fitness score for the experiment identified by the record ID.
   */
  public static double compute(SubjectStateBridge bridge, GroningenConfig config) {
    PauseTime pauseTime = bridge.getPauseTime();
    ResourceMetric resourceMetric = bridge.getResourceMetric();
    GroningenParamsOrBuilder params = config.getParamBlock();

    // TODO(team): better way of passing percentiles thru Subject?
    pauseTime.setPercentile(params.getPauseTimeLatencyScorePercentile());

    final String subjectSignature = bridge.getHumanIdentifier();

    double a = checkValue(subjectSignature, "a", params.getLatencyWeight());
    double x = checkValue(subjectSignature, "x",
        pauseTime.computeScore(PauseTime.ScoreType.LATENCY));
    double b = checkValue(subjectSignature, "b", params.getThroughputWeight());
    double y = checkValue(subjectSignature, "y",
        pauseTime.computeScore(PauseTime.ScoreType.THROUGHPUT));
    double c = checkValue(subjectSignature, "c", params.getMemoryWeight());
    double z = checkValue(subjectSignature, "z",
        resourceMetric.computeScore(ResourceMetric.ScoreType.MEMORY));
    double result = checkValue(subjectSignature, "result (a * x + b * y + c * z)",
        a * x + b * y + c * z);

    final String subjectIndex = Integer.toString(bridge.getAssociatedSubject().getSubjectIndex());

    return result;
  }

  /** Checks that the double value is non-negative and not NaN */
  private static double checkValue(final String subject, final String scoreName,
      final double value) {
    if ((value >= 0.0) && (Double.isNaN(value) == false)) {
      log.info(String.format("%s Valid score: %s == %.10f", subject, scoreName, value));
      return value;
    } else {
      log.warning(
          String.format("%s FitnessScore anomaly detected: %s %s", subject, scoreName, value));
      return 0.0;
    }
  }
}
