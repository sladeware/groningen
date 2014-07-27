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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ProtocolMessageEnum;

import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.SimpleScope;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.NamedConfigParamImpl;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;

import java.util.logging.Logger;

/**
 * Guice module that handles Groningen's per-pipeline configuration parameters.
 */
public class GroningenConfigParamsModule extends AbstractModule {
  private static final Logger log =
    Logger.getLogger(GroningenConfigParamsModule.class.getCanonicalName());

  /**
   * Seeds one particular configuration parameter of the given type into the given scope.
   * Suppressing "unchecked" warnings due to generic type being used for type casting.
   */
  @SuppressWarnings("unchecked")
  private static <T> void nailConfigParamToScope(Class<T> type, FieldDescriptor fd,
      GroningenConfig config, BlockScope scope) {
    T value = (T) config.getParamBlock().getField(fd);
    log.fine(
        String.format("nailing %s (%s) to {%s}", fd.getName(), fd.getJavaType().name(), value));

    scope.seed(Key.get(type, new NamedConfigParamImpl(fd.getName())), value);
  }

  /**
   * Binds given type annotated by the name of the FieldDescriptor to a
   * SimpleScope.seededKeyProvider. This provider throws an exception when the value is
   * injected outside of the desired scope.
   */
  private <T> void bindConfigParamToSeededKeyProvider(Class<T> type, FieldDescriptor fd) {
    bind(Key.get(type, new NamedConfigParamImpl(fd.getName())))
      .toProvider(SimpleScope.<T>seededKeyProvider())
      .in(PipelineIterationScoped.class);
  }

  /**
   * Nails all the per-pipeline configuration parameters to a given Guice scope.
   *
   * @param config Configuration parameters to be fixed for the pipeline run duration.
   * @param scope Guice scope used to fix the parameters.
   */
  public static void nailConfigToScope(GroningenConfig config, BlockScope scope) {
    scope.seed(GroningenConfig.class, config);

    for (FieldDescriptor fd : GroningenParams.getDescriptor().getFields()) {
      switch (fd.getJavaType()) {
        case ENUM:
          nailConfigParamToScope(ProtocolMessageEnum.class, fd, config, scope);
          break;
        case INT:
          nailConfigParamToScope(Integer.class, fd, config, scope);
          break;
        case LONG:
          nailConfigParamToScope(Long.class, fd, config, scope);
          break;
        case DOUBLE:
          nailConfigParamToScope(Double.class, fd, config, scope);
          break;
        case STRING:
          nailConfigParamToScope(String.class, fd, config, scope);
          break;
        default:
          log.warning(String.format("unrecognized field descriptor type: %s.", fd.getJavaType()));
          break;
      }
    }
  }

  /**
   * GroningenConfigParamsModule binds pipelineIterationScope to a specific SimpleScope
   * implementation and binds all the configuration parameters (fields annotated with
   * @NamedConfigParam) to a SimpleScope.seededKeyProvider(), which prevents them being
   * injected outside of the scope (see bindConfigParamToSeededKeyProvider()).
   */
  @Override
  protected void configure() {
    bind(GroningenConfig.class)
      .toProvider(SimpleScope.<GroningenConfig>seededKeyProvider())
      .in(PipelineIterationScoped.class);

    for (FieldDescriptor fd : GroningenParams.getDescriptor().getFields()) {
      switch (fd.getJavaType()) {
        case ENUM:
          bindConfigParamToSeededKeyProvider(ProtocolMessageEnum.class, fd);
        break;
        case INT:
          bindConfigParamToSeededKeyProvider(Integer.class, fd);
          break;
        case LONG:
          bindConfigParamToSeededKeyProvider(Long.class, fd);
          break;
        case DOUBLE:
          bindConfigParamToSeededKeyProvider(Double.class, fd);
          break;
        case STRING:
          bindConfigParamToSeededKeyProvider(String.class, fd);
          break;
        default:
          log.warning(String.format("unrecognized field descriptor type: %s.", fd.getJavaType()));
          break;
      }
    }
  }
}
