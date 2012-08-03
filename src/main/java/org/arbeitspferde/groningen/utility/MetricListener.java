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

/**
 * Provide a means of exposing some telemetry source's values to a {@link MetricExporter} for
 * consumption.
 *
 * @param <T> The type of metric that will be exposed.
 */
public interface MetricListener<T> {
  /**
   * Emit the instantaneous telemetry source's value.
   */
  public T value();
}
