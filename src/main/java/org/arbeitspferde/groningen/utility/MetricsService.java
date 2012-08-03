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

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.logging.Logger;

@Singleton
public class MetricsService extends AbstractIdleService {
  private static final Logger log = Logger.getLogger(MetricsService.class.getCanonicalName());

  private final MetricExporter metricExporter;

  @Inject
  public MetricsService(final MetricExporter metricExporter) {
    this.metricExporter = metricExporter;
  }

  @Override
  protected void startUp() throws Exception {
    log.info("Starting metrics export service.");
    metricExporter.init();
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("Stopping metrics export service.");
  }
}
