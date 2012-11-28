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

/**
 * Tests for {@link Formatters}.
 */
public class FormattersTest extends TestCase {
  public void test_INTEGER_FORMATTER_asArgumentString_DisallowsNullArguments() {
    try {
      Formatters.INTEGER_FORMATTER.asArgumentString(null, 0L);
      fail("Formatters.INTEGER_FORMATTER should disallow null arguments.");
    } catch (final NullPointerException e) {
    }
  }

  public void test_INTEGER_FORMATTER_asArgumentString_AdaptiveSizeDecrementScaleFactor() {
    final String actual = Formatters.INTEGER_FORMATTER.asArgumentString(
        JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 2L);

    assertEquals("-XX:AdaptiveSizeDecrementScaleFactor=2", actual);
  }

  public void test_INTEGER_FORMATTER_asArgumentString_MaxNewSize() {
    final String actual = Formatters.INTEGER_FORMATTER.asArgumentString(
        JvmFlag.MAX_NEW_SIZE, 3L);

    assertEquals("-XX:MaxNewSize=3m", actual);
  }

  public void test_INTEGER_FORMATTER_asRegularExpression_AdaptiveSizeDecrementFactor() {
    final String actual = Formatters.INTEGER_FORMATTER.asRegularExpressionString(
        JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR);

    assertEquals("-XX:AdaptiveSizeDecrementScaleFactor=\\d+\\b", actual);
  }

  public void test_INTEGER_FORMATTER_asRegularExpression_MaxNewSize() {
    final String actual = Formatters.INTEGER_FORMATTER.asRegularExpressionString(
        JvmFlag.MAX_NEW_SIZE);

    assertEquals("-XX:MaxNewSize=\\d+[bBkKmMgG]\\b", actual);
  }

  public void test_INTEGER_FORMATTER_asAcceptableValuesString_AdaptiveSizeDecrementFactor() {
    final String actual = Formatters.INTEGER_FORMATTER.asAcceptableValuesString(
        JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR);

    assertEquals("[0\u2025100]", actual);
  }

  public void test_INTEGER_FORMATTER_asAcceptableValuesString_MaxNewSize() {
    final String actual = Formatters.INTEGER_FORMATTER.asAcceptableValuesString(
        JvmFlag.MAX_NEW_SIZE);

    assertEquals("[0\u202532768]", actual);
  }

  public void test_INTEGER_FORMATTER_validate_AdaptiveSizeDecrementFactor_InvalidValues() {
    try {
      Formatters.INTEGER_FORMATTER.validate(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, -1L);
      fail("JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR disallows -1.");
    } catch (final IllegalArgumentException e) {
    }

    try {
      Formatters.INTEGER_FORMATTER.validate(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 101L);
      fail("JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR disallows 101.");
    } catch (final IllegalArgumentException e) {
    }
  }

  public void test_INTEGER_FORMATTER_validate_AdaptiveSizeDecrementFactor_ValidValues() {
    try {
      Formatters.INTEGER_FORMATTER.validate(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 1L);
    } catch (final IllegalArgumentException e) {
      fail("JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR allows 1.");
    }

    try {
      Formatters.INTEGER_FORMATTER.validate(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 50L);
    } catch (final IllegalArgumentException e) {
      fail("JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR allows 50");
    }

    try {
      Formatters.INTEGER_FORMATTER.validate(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 100L);
    } catch (final IllegalArgumentException e) {
      fail("JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR allows 100");
    }
  }

  public void test_BOOLEAN_FORMATTER_asArgumentString_DisallowsNullArguments() {
    try {
      Formatters.BOOLEAN_FORMATTER.asArgumentString(null, 0L);
      fail("Formatters.BOOLEAN_FORMATTER should disallow null arguments.");
    } catch (final NullPointerException e) {
    }
  }

  public void test_BOOLEAN_FORMATTER_asArgumentString_CMSIncrementalMode_true() {
    final String actual = Formatters.BOOLEAN_FORMATTER.asArgumentString(
      JvmFlag.CMS_INCREMENTAL_MODE, 1L);

    assertEquals("-XX:+CMSIncrementalMode", actual);
  }

  public void test_BOOLEAN_FORMATTER_asArgumentString_CMSIncrementalMode_false() {
    final String actual = Formatters.BOOLEAN_FORMATTER.asArgumentString(
      JvmFlag.CMS_INCREMENTAL_MODE, 0L);

    assertEquals("-XX:-CMSIncrementalMode", actual);
  }

  public void test_BOOLEAN_FORMATTER_asRegularExpression_CMSIncrementalMode() {
    final String actual = Formatters.BOOLEAN_FORMATTER.asRegularExpressionString(
      JvmFlag.CMS_INCREMENTAL_MODE);

    assertEquals("-XX:[+-]CMSIncrementalMode", actual);
  }

  public void test_BOOLEAN_FORMATTER_asAcceptableValuesString_CMSIncrementalMode() {
    final String actual = Formatters.BOOLEAN_FORMATTER.asAcceptableValuesString(
      JvmFlag.CMS_INCREMENTAL_MODE);

    assertEquals("{0 (false), 1 (true)}", actual);
  }

  public void test_BOOLEAN_FORMATTER_validate_CMSIncrementalMode_InvalidValues() {
    try {
      Formatters.BOOLEAN_FORMATTER.validate(JvmFlag.CMS_INCREMENTAL_MODE, -1L);
      fail("JvmFlags.CMS_INCREMENTAL_MODE disallows -1.");
    } catch (final IllegalArgumentException e) {
    }

    try {
      Formatters.BOOLEAN_FORMATTER.validate(JvmFlag.CMS_INCREMENTAL_MODE, 2L);
      fail("JvmFlags.CMS_INCREMENTAL_MODE disallows 2.");
    } catch (final IllegalArgumentException e) {
    }
  }

  public void test_BOOLEAN_FORMATTER_validate_CMSIncrementalMode_ValidValues() {
    try {
      Formatters.BOOLEAN_FORMATTER.validate(JvmFlag.CMS_INCREMENTAL_MODE, 0L);
    } catch (final IllegalArgumentException e) {
      fail("JvmFlags.CMS_INCREMENTAL_MODE allows 0.");
    }

    try {
      Formatters.BOOLEAN_FORMATTER.validate(JvmFlag.CMS_INCREMENTAL_MODE, 1L);
    } catch (final IllegalArgumentException e) {
      fail("JvmFlags.CMS_INCREMENTAL_MODE allows 1.");
    }
  }
}
