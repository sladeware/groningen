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
import com.google.inject.Key;
import com.google.inject.Scope;

import javax.annotation.Nullable;

/**
 * A type of {@link Scope} that is useful for wrapping around arbitrary blocks of code.
 */
public interface BlockScope extends Scope {
  public void enter();
  public void exit();
  public boolean isInScope();
  public <T> void seed(final Key<T> key, @Nullable final T value);
  public <T> void seed(final Class<T> clazz, @Nullable final T value);
}
