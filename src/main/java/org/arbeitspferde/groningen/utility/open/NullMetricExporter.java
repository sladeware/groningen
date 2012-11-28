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

package org.arbeitspferde.groningen.utility.open;

import com.google.inject.Singleton;
import org.arbeitspferde.groningen.utility.MetricExporter;
import org.arbeitspferde.groningen.utility.MetricListener;

/**
 * A dummy implementation of {@link MetricExporter} that does nothing.
 */
@Singleton
public class NullMetricExporter implements MetricExporter {

  @Override
  public void register(final String name, final String description,
      final MetricListener<?> metric) {
  }

  @Override
  public void init() {
  }
}
