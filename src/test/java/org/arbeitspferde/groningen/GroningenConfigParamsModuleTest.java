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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;

import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.open.OpenModule;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for {@link GroningenConfigParamsModule}.
 */
public class GroningenConfigParamsModuleTest extends TestCase {
  private static final Logger log =
      Logger.getLogger(GroningenConfigParamsModuleTest.class.getCanonicalName());

  private Injector injector;
  private BlockScope scope;

  private static class SampleClass {
    @Inject
    @NamedConfigParam("duration")
    public int duration;
  }

  @Override
  public void setUp() {
    final String[] args = new String[] {};

    injector = Guice.createInjector(new ServicesModule(), new BaseModule(args),
        new GroningenConfigParamsModule(), new OpenModule());
    scope = injector.getInstance(Key.get(BlockScope.class,
        Names.named(PipelineIterationScoped.SCOPE_NAME)));
  }

  /**
   * Tests that ProvisionException is thrown when when nailConfigToScope() is not called
   */
  public void testThrowsIfNailConfigToScopeWasNotCalled() {
    try {
      SampleClass sampleClass = injector.getInstance(SampleClass.class);
      fail("No ProvisionException has been thrown");
    } catch (ProvisionException e) {
      log.log(Level.FINE, "ProvisionException thrown", e);
    }
  }

  /**
   * Tests that OutOfScopeException is thrown when NamedConfigParam is injected outside of the
   * pipelineIterationScope.
   */
  public void testThrowsWhenNamedConfigParamIsInjectedOutsideOfScope() {
    try {
      GroningenConfig config = new StubConfigManager.StubConfig() {{
        paramBlock = GroningenParams.newBuilder().setDuration(42).buildPartial();
      }};
      GroningenConfigParamsModule.nailConfigToScope(config, scope);

      SampleClass sampleClass = injector.getInstance(SampleClass.class);
      fail("No ProvisionException has been thrown");
    } catch (OutOfScopeException e) {
      log.log(Level.FINE, "OutOfScopeException thrown", e);
    }
  }

  /**
   * Tests that GroningenConfigParamsModule.nailConfigToScope() works correctly and seeds
   * configuration parameters into the current scope.
   */
  public void testCorrectlyInjectsNamedConfigParamInsideTheScope() {
    scope.enter();
    try {
      GroningenConfig config = new StubConfigManager.StubConfig() {{
        paramBlock = GroningenParams.newBuilder().setDuration(42).buildPartial();
      }};
      GroningenConfigParamsModule.nailConfigToScope(config, scope);

      SampleClass sampleClass = injector.getInstance(SampleClass.class);
      assertEquals(42, sampleClass.duration);
    } finally {
      scope.exit();
    }
  }

  /**
   * Tests that when the scope is reentered and GroningenConfigParamsModule.nailConfigToScope()
   * is called second time, new values of the configuration parameters are injected
   * successfully.
   */
  public void testReinjectsNamedConfigParamWhenTheScopeIsReentered() {
    scope.enter();
    try {
      GroningenConfig config = new StubConfigManager.StubConfig() {{
        paramBlock = GroningenParams.newBuilder().setDuration(42).buildPartial();
      }};
      GroningenConfigParamsModule.nailConfigToScope(config, scope);

      SampleClass sampleClass = injector.getInstance(SampleClass.class);
      assertEquals(42, sampleClass.duration);
    } finally {
      scope.exit();
    }

    scope.enter();
    try {
      GroningenConfig config = new StubConfigManager.StubConfig() {{
        paramBlock = GroningenParams.newBuilder().setDuration(43).buildPartial();
      }};
      GroningenConfigParamsModule.nailConfigToScope(config, scope);

      SampleClass sampleClass = injector.getInstance(SampleClass.class);
      assertEquals(43, sampleClass.duration);
    } finally {
      scope.exit();
    }
  }

  /**
   * Tests that Guice scope and GroningenConfigParamsModule.nailConfigToScope() calls are working
   * correctly when used with different configurations from different threads.
   */
  public void testCorrectlyInjectsDifferentValuesForDifferentThreads()
    throws InterruptedException {

    final ArrayList<Exception> threadExceptions = new ArrayList<>();
    final Thread thread1 = new Thread() {
      @Override
      public void run() {
        scope.enter();
        try {
          GroningenConfig config = new StubConfigManager.StubConfig() {{
            paramBlock = GroningenParams.newBuilder().setDuration(42).buildPartial();
          }};
          GroningenConfigParamsModule.nailConfigToScope(config, scope);

          SampleClass sampleClass = injector.getInstance(SampleClass.class);
          assertEquals(42, sampleClass.duration);
        } catch (Exception e) {
          threadExceptions.add(e);
        } finally {
          scope.exit();
        }
      }
    };

    final Thread thread2 = new Thread() {
      @Override
      public void run() {
        scope.enter();
        try {
          GroningenConfig config = new StubConfigManager.StubConfig() {{
            paramBlock = GroningenParams.newBuilder().setDuration(43).buildPartial();
          }};
          GroningenConfigParamsModule.nailConfigToScope(config, scope);

          thread1.start();
          thread1.join();

          SampleClass sampleClass = injector.getInstance(SampleClass.class);
          assertEquals(43, sampleClass.duration);
        } catch (Exception e) {
          threadExceptions.add(e);
        } finally {
          scope.exit();
        }
      }
    };

    thread2.start();
    thread2.join();

    if (!threadExceptions.isEmpty()) {
      fail(threadExceptions.get(0).getMessage());
    }
  }
}
