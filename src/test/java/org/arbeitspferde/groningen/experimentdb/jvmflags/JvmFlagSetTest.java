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
 * Tests for {@link JvmFlagSet}.
 */
public class JvmFlagSetTest extends TestCase {
  public void testBuilder_CreateAlmostEmptySet() {
    final JvmFlagSet.Builder jvmFlagSetBuilder = JvmFlagSet.builder();
    assertNotNull(jvmFlagSetBuilder);

    jvmFlagSetBuilder.withValue(JvmFlag.USE_SERIAL_GC, 1);

    final JvmFlagSet jvmFlagSet = jvmFlagSetBuilder.build();
    assertNotNull(jvmFlagSet);
  }

  public void testBuilder_ValidateAndSetValidFlagAndYieldResult() {
    final JvmFlagSet.Builder jvmFlagSetBuilder = JvmFlagSet.builder();

    try {
      jvmFlagSetBuilder.withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 1L)
          .withValue(JvmFlag.USE_SERIAL_GC, 1);

      final JvmFlagSet jvmFlagSet = jvmFlagSetBuilder.build();

      assertNotNull(jvmFlagSet);

      assertEquals(1L, jvmFlagSet.getValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR));
      assertEquals(1L, jvmFlagSet.getValue(JvmFlag.USE_SERIAL_GC));

    } catch (final IllegalArgumentException e) {
      fail("Should have validated value " + e);
    } catch (final IllegalStateException e) {
      fail("Should have had semantically correct FlagSet construct " + e);
    }
  }

  public void testBuilder_ValidateValidSemantics() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.USE_SERIAL_GC, 1)
        .withValue(JvmFlag.HEAP_SIZE, 100L)
        .withValue(JvmFlag.MAX_NEW_SIZE, 50L);

    try {
      builder.build();
    } catch (final IllegalStateException e) {
      fail("Should have had semantically correct FlagSet construct " + e);
    }
  }

  public void testBuilder_ValidateInvalidSemantics_NewLargerThanHeap() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.USE_SERIAL_GC, 1)
        .withValue(JvmFlag.HEAP_SIZE, 50L)
        .withValue(JvmFlag.MAX_NEW_SIZE, 100L);

    builder.build();
  }

  public void testBuilder_ValidateInvalidSemantics_GCModeUnset() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.HEAP_SIZE, 100L)
        .withValue(JvmFlag.MAX_NEW_SIZE, 50L);

    builder.build();
  }

  public void testBuilder_ValidateInvalidSemantics_GCModeSetButInvalid() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.HEAP_SIZE, 100L)
        .withValue(JvmFlag.MAX_NEW_SIZE, 50L)
        .withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, 0)
        .withValue(JvmFlag.USE_PARALLEL_GC, 0)
        .withValue(JvmFlag.USE_PARALLEL_OLD_GC, 0)
        .withValue(JvmFlag.USE_SERIAL_GC, 0);

    builder.build();
  }

  public void testBuilder_CreatesDuplicate() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.USE_SERIAL_GC, 1)
        .withValue(JvmFlag.HEAP_SIZE, 100L)
        .withValue(JvmFlag.MAX_NEW_SIZE, 50L);

    try {
      final JvmFlagSet flagSet1 = builder.build();
      final JvmFlagSet flagSet2 = builder.build();

      assertNotNull(flagSet1);
      assertNotNull(flagSet2);
      assertNotSame(flagSet2, flagSet1);
    } catch (final IllegalStateException e) {
      fail("Should have had semantically correct FlagSet construct " + e);
    }
  }
}
