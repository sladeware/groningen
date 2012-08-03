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
 * Displays an object using its user-provided information string and by calling
 * the object's toString
 */
public class DisplayableObject implements Displayable {
  protected final Object object;
  protected final String infoString;

  /**
   * Constructor
   *
   * @param object a reference to the {@link Object} to be displayed
   * @param infoString user specified string to be displayed in association
   * with <code>object.toString()</code>
   */
  public DisplayableObject (Object object, String infoString) {
    this.object = object;
    this.infoString = infoString;
  }

  /**
   * Displays the number and its <code>infoString</code>
   *
   * @return An HTML string
   */
  @Override
  public String toHtml() {
    return infoString + ": " + object.toString() + "<br>";
  }

}
