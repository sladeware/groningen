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

package org.arbeitspferde.groningen.display;

/**
 * Provides a mechanism to output formatted html representing some view of
 * information. The Displayable interface should be used by any class that
 * displays on the GroningenServlet.
 */
public interface Displayable {
  /**
   * Returns an HTML string to display on {@link GroningenServlet}
   */
  public String toHtml();
}
