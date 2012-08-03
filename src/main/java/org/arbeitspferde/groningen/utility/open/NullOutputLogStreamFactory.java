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

import org.arbeitspferde.groningen.utility.OutputLogStream;
import org.arbeitspferde.groningen.utility.OutputLogStreamFactory;

import java.io.OutputStream;

/**
 * A simple factory that does NOT yet create {@link OutputLogStream} instances.
 */
@Singleton
public class NullOutputLogStreamFactory implements OutputLogStreamFactory {
  @Override
  public OutputLogStream forStream(final OutputStream stream) {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public OutputLogStream rotatingStreamForSpecification(final Specification specification) {
    throw new RuntimeException("Not implemented.");
  }
}
