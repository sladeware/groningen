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

package org.arbeitspferde.groningen.experimentdb.jvmflags;


import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for {@link JvmFlag}.
 */
public class JvmFlagTest extends TestCase {
  public void test_asArgumentString_MAX_NEW_SIZE() {
    final String actual = JvmFlag.MAX_NEW_SIZE.asArgumentString(1L);

    assertEquals("-XX:MaxNewSize=1m", actual);
  }

  public void test_asRegularExpressionString_MAX_NEW_SIZE() {
    final String actual = JvmFlag.MAX_NEW_SIZE.asRegularExpressionString();

    assertEquals("-XX:MaxNewSize=\\d+[bBkKmMgG]\\b", actual);
  }

  public void test_validate_MAX_NEW_SIZE_InvalidValues() {
    try {
      JvmFlag.MAX_NEW_SIZE.validate(-1L);
      fail("JvmFlag.JAVA_HEAP_MAXIMUM_SIZE should disallow -1.");
    } catch (final IllegalArgumentException expected) {
    }

    try {
      JvmFlag.MAX_NEW_SIZE.validate(65537L);
      fail("JvmFlag.JAVA_HEAP_MAXIMUM_SIZE should disallow 65537.");
    } catch (final IllegalArgumentException expected) {
    }
  }

  public void test_validate_MAX_NEW_SIZE_ValidValues() {
    try {
      JvmFlag.MAX_NEW_SIZE.validate(1L);
    } catch (final IllegalArgumentException expected) {
      fail("JvmFlag.JAVA_HEAP_MAXIMUM_SIZE should allow 1.");
    }

    try {
      JvmFlag.MAX_NEW_SIZE.validate(2048L);
    } catch (final IllegalArgumentException expected) {
      fail("JvmFlag.JAVA_HEAP_MAXIMUM_SIZE should allow 2048.");
    }
  }

  public void test_getGcModeArgument_EmitsExpected() {
    assertEquals(JvmFlag.USE_CONC_MARK_SWEEP_GC, JvmFlag.getGcModeArgument(GcMode.CMS));
    assertEquals(JvmFlag.USE_PARALLEL_GC, JvmFlag.getGcModeArgument(GcMode.PARALLEL));
    assertEquals(JvmFlag.USE_PARALLEL_OLD_GC, JvmFlag.getGcModeArgument(GcMode.PARALLEL_OLD));
    assertEquals(JvmFlag.USE_SERIAL_GC, JvmFlag.getGcModeArgument(GcMode.SERIAL));
  }

  public void test_asAcceptableValuesString_MAX_NEW_SIZE() {
    final String actual = JvmFlag.MAX_NEW_SIZE.asAcceptableValuesString();

    assertEquals("[0\u202532768]", actual);
  }
}
