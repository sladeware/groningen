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

package org.arbeitspferde.groningen;

import com.google.common.hash.HashFunction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.utility.Clock;

/**
 * Generates PipelineId based on serving address, current time and given current configuration.
 */
@Singleton
public class PipelineIdGenerator {
  private final String servingAddress;
  private final Clock clock;
  private final HashFunction hashFunction;

  @Inject
  public PipelineIdGenerator(@Named("servingAddress") final String systemServingAddress,
      final Clock clock, final HashFunction hashFunction) {
    this.servingAddress = systemServingAddress;
    this.clock = clock;
    this.hashFunction = hashFunction;
  }

  /**
   * Generates unique pipeline id based on the given config, subject serving address and current
   * time.
   *
   * @param config {@link GroningenConfig} used for hashing
   * @return generated {@link PipelineId}
   */
  public PipelineId generatePipelineId(GroningenConfig config) {
    StringBuilder sb = new StringBuilder(servingAddress);
    sb.append("_");
    sb.append(clock.now().getMillis());
    sb.append("_");
    sb.append(config.getProtoConfig().toString());
    return new PipelineId(hashFunction.hashString(sb).toString());
  }
}
