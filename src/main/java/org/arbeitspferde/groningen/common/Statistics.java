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

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * The Groningen statistics class contains all the specialized statistics routines
 * required to support functionality such as scoring latency.
 *
 *
 * TODO(team) implement linear interpolation, or use com.google.math.Rank
 */
public class Statistics {
  /** Logger for this class */
  private static final Logger log = Logger.getLogger(Statistics.class.getCanonicalName());

  /**
   * Compute the input percentile of the input double {@link List}. Returns
   * Double.NaN if the list of values is empty.
   *
   * Keep in mind that the values are sorted in place, thus modifying the values
   * {@link List} you've passed in.
   *
   * @param {@link Double} {@link List}
   * @param double percentile
   */
  public static double computePercentile(List<Double> values, double percentile) {
    Preconditions.checkArgument(percentile >= 0, "percentile must be greater or equal than 0");
    Preconditions.checkArgument(percentile <= 100, "percentile must be smaller or equal than 100");

    int sz = values.size();
    switch (sz) {
      case 0:
        log.warning("The list passed to computePercentile is empty. NaN returned.");
        return Double.NaN;
      case 1:
        return values.get(0);
      default:
        // Sort {@link List} in place and in ascending order
        Collections.sort(values);
        // the 100th percentile is defined to be the largest value
        if (percentile == 100){
          return values.get(sz - 1);
        }
        return values.get(
            (int) Math.floor(percentile * (double) sz / 100.0));
    }
  }
}
