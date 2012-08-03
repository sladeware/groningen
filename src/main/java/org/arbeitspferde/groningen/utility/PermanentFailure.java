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
 * A Failure case that is used to denote that underlying operation cannot simply be retried.
 *
 * For instance, this may occur if a user issues a fundamentally invalid request that could
 * never, even in a perfect world, be served.
 *
 */
public class PermanentFailure extends Exception {
  public PermanentFailure(final String message) {
    super(message);
  }

  public PermanentFailure(final String message, final Throwable e) {
    super(message, e);
  }

  public PermanentFailure(final Throwable e) {
    super(e);
  }
}
