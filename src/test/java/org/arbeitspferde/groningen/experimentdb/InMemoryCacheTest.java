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

package org.arbeitspferde.groningen.experimentdb;


import junit.framework.TestCase;

import org.arbeitspferde.groningen.experimentdb.InMemoryCache;

/**
 * The test for {@link InMemoryCache}.
 */
public class InMemoryCacheTest extends TestCase {
  private static final int TEST_ID = 234;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testRegisterAndLookup() throws Exception {
    TestInMemoryCache cache = new TestInMemoryCache();
    TestValue value = new TestValue(TEST_ID);

    assertEquals(null, cache.lookup(TEST_ID));
    TestValue stillValue = cache.register(value);
    assertSame(value, stillValue);

    assertSame(value, cache.lookup(TEST_ID));
    assertEquals(null, cache.lookup(TEST_ID + 1));
  }

  static class TestInMemoryCache extends InMemoryCache<TestValue> {}

  static class TestValue extends InMemoryCache.Value<TestValue> {
    public TestValue(long id) {
      super(id);
    }
  }
}
