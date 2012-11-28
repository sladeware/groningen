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

package org.arbeitspferde.groningen.utility.logstream;

import com.google.protobuf.Message;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * A {@link OutputLogStream} is responsible for encapsulating Protocol Buffer
 * emissions in a record-oriented format that can be processed ex post facto.
 */
@NotThreadSafe
public interface OutputLogStream extends Flushable, Closeable {
  public void write(final Message message) throws IOException;
}
