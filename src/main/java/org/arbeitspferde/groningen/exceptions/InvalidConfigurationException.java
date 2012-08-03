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

package org.arbeitspferde.groningen.exceptions;

/**
 * The configuration object specified is not valid, ie it is under specified or missing a
 * required piece of information, or is internally inconsistent, or is incorrect in
 * some other way explained in the message
 */
public class InvalidConfigurationException extends Exception {
  public InvalidConfigurationException() {
    super();
  }

  public InvalidConfigurationException(String message) {
    super(message);
  }

}
