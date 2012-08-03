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

package org.arbeitspferde.groningen.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.config.ProtoBufSearchSpaceBundle;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.JvmSearchSpace;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.JvmSearchSpace.Int64Range;

/**
 * Tests for ProtoBufSearchSpaceBundle.
 */
public class ProtoBufSearchSpaceBundleTest extends TestCase {

  public void testConstructionForDefaultWithNoRequireAll() throws Exception {
    JvmSearchSpace searchSpace = JvmSearchSpace.newBuilder()
      .addGcMode(JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP)
      .build();

    ProtoBufSearchSpaceBundle bundle = new ProtoBufSearchSpaceBundle(searchSpace, false);
    assertEquals("new ratio floor", JvmFlag.NEW_RATIO.getMinimum(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getFloor());
    assertEquals("new ratio ceiling", JvmFlag.NEW_RATIO.getMaximum(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getCeiling());
    assertEquals("new ratio step size", JvmFlag.NEW_RATIO.getStepSize(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getStepSize());
    assertEquals("pacing floor", 0L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getFloor());
    assertEquals("pacing ceiling", 1L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getCeiling());
    assertEquals("pacing step size", 1L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getStepSize());

  }

  public void testConstructionInt64RangesWithRangesAndNoRequireAll() throws Exception {
    JvmSearchSpace searchSpace = JvmSearchSpace.newBuilder()
      .addGcMode(JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP)
      .setGcTimeRatio(JvmSearchSpace.Int64Range.newBuilder()
          .setFloor(0L)
          .setCeiling(100L)
          .setStepSize(10L)
          .build())
      .setTenuredGenerationSizeIncrement(JvmSearchSpace.Int64Range.newBuilder()
          .setFloor(3L)
          .setCeiling(99L)
          .setStepSize(3L)
          .build())
      .build();

    ProtoBufSearchSpaceBundle bundle = new ProtoBufSearchSpaceBundle(searchSpace, false);
    assertEquals("gc ratio floor", searchSpace.getGcTimeRatio().getFloor(),
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getFloor());
    assertEquals("gc ratio ceiling", searchSpace.getGcTimeRatio().getCeiling(),
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getCeiling());
    assertEquals("gc ratio step size", searchSpace.getGcTimeRatio().getStepSize(),
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getStepSize());
    assertEquals("tenured floor",
        searchSpace.getTenuredGenerationSizeIncrement().getFloor(),
        bundle.getSearchSpace(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT).getFloor());
    assertEquals("tenured ceiling",
        searchSpace.getTenuredGenerationSizeIncrement().getCeiling(),
        bundle.getSearchSpace(
            JvmFlag.TENURED_GENERATION_SIZE_INCREMENT).getCeiling());
    assertEquals("tenured step size",
        searchSpace.getTenuredGenerationSizeIncrement().getStepSize(),
        bundle.getSearchSpace(
            JvmFlag.TENURED_GENERATION_SIZE_INCREMENT).getStepSize());
    assertEquals("default new ratio floor", JvmFlag.NEW_RATIO.getMinimum(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getFloor());
    assertEquals("default new ratio ceiling", JvmFlag.NEW_RATIO.getMaximum(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getCeiling());
    assertEquals("default new ratio step size", JvmFlag.NEW_RATIO.getStepSize(),
        bundle.getSearchSpace(JvmFlag.NEW_RATIO).getStepSize());
  }

  public void testConstructionInt64RangesWithValueAndNoRequireAll() throws Exception {
    JvmSearchSpace searchSpace = JvmSearchSpace.newBuilder()
      .addGcMode(JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP)
      .setGcTimeRatio(JvmSearchSpace.Int64Range.newBuilder()
          .setValue(50L)
          .build())
      .build();

    ProtoBufSearchSpaceBundle bundle = new ProtoBufSearchSpaceBundle(searchSpace, false);
    assertEquals("gc ratio floor", searchSpace.getGcTimeRatio().getValue(),
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getFloor());
    assertEquals("gc ratio ceiling", searchSpace.getGcTimeRatio().getValue(),
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getCeiling());
    assertEquals("gc ratio step size", 0L,
        bundle.getSearchSpace(JvmFlag.GC_TIME_RATIO).getStepSize());
  }

  public void testConstructionBooleanWithValueAndNoRequireAll() throws Exception {
    JvmSearchSpace searchSpace = JvmSearchSpace.newBuilder()
      .addGcMode(JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP)
      .setCmsIncrementalPacing(false)
      .build();

    ProtoBufSearchSpaceBundle bundle = new ProtoBufSearchSpaceBundle(searchSpace, false);
    assertEquals("pacing floor", 0L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getFloor());
    assertEquals("pacing ceiling", 0L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getCeiling());
    assertEquals("pacing step size", 0L,
        bundle.getSearchSpace(JvmFlag.CMS_INCREMENTAL_PACING).getStepSize());
  }

  /*
   *  TODO(team): Data-driven testing is difficult to debug and somewhat dangerous.  Consider
   *              rewriting this with hardcoded definitions to ensure conformance.
   */
  public void testConstructionWithRequireAll() throws Exception {
    ImmutableMap<JvmFlag, Descriptors.FieldDescriptor> argToProtoFieldMap =
        ImmutableMap.<JvmFlag, Descriptors.FieldDescriptor>builder()
            .put(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR_FIELD_NUMBER))
            .put(JvmFlag.CMS_EXP_AVG_FACTOR,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_EXP_AVG_FACTOR_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_DUTY_CYCLE_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_DUTY_CYCLE_MIN_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_OFFSET,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_OFFSET_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_SAFETY_FACTOR_FIELD_NUMBER))
            .put(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INITIATING_OCCUPANCY_FRACTION_FIELD_NUMBER))
            .put(JvmFlag.GC_TIME_RATIO,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.GC_TIME_RATIO_FIELD_NUMBER))
            .put(JvmFlag.MAX_GC_PAUSE_MILLIS,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.MAX_GC_PAUSE_MILLIS_FIELD_NUMBER))
            .put(JvmFlag.MAX_HEAP_FREE_RATIO,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.MAX_HEAP_FREE_RATIO_FIELD_NUMBER))
            .put(JvmFlag.MIN_HEAP_FREE_RATIO,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.MIN_HEAP_FREE_RATIO_FIELD_NUMBER))
            .put(JvmFlag.NEW_RATIO,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.NEW_RATIO_FIELD_NUMBER))
            .put(JvmFlag.NEW_SIZE,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.NEW_SIZE_FIELD_NUMBER))
            .put(JvmFlag.MAX_NEW_SIZE,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.MAX_NEW_SIZE_FIELD_NUMBER))
            .put(JvmFlag.PARALLEL_GC_THREADS,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.PARALLEL_GC_THREADS_FIELD_NUMBER))
            .put(JvmFlag.SURVIVOR_RATIO,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.SURVIVOR_RATIO_FIELD_NUMBER))
            .put(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.TENURED_GENERATION_SIZE_INCREMENT_FIELD_NUMBER))
            .put(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.YOUNG_GENERATION_SIZE_INCREMENT_FIELD_NUMBER))
            .put(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.SOFT_REF_LRU_POLICY_MS_PER_MB_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_MODE,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_MODE_FIELD_NUMBER))
            .put(JvmFlag.CMS_INCREMENTAL_PACING,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.CMS_INCREMENTAL_PACING_FIELD_NUMBER))
            .put(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.USE_CMS_INITIATING_OCCUPANCY_ONLY_FIELD_NUMBER))
            .put(JvmFlag.USE_CONC_MARK_SWEEP_GC,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.GC_MODE_FIELD_NUMBER))
            .put(JvmFlag.USE_PARALLEL_GC,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.GC_MODE_FIELD_NUMBER))
            .put(JvmFlag.USE_PARALLEL_OLD_GC,
                JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.GC_MODE_FIELD_NUMBER))
            .put(JvmFlag.USE_SERIAL_GC,
                 JvmSearchSpace.getDescriptor().findFieldByNumber(
                    JvmSearchSpace.GC_MODE_FIELD_NUMBER))
            .put(JvmFlag.HEAP_SIZE,
                 JvmSearchSpace.getDescriptor().findFieldByNumber(
                   JvmSearchSpace.HEAP_SIZE_FIELD_NUMBER))

            .build();

    ImmutableList<JvmFlag> boolCmdLineArguments =
        ImmutableList.<JvmFlag>builder()
            .add(JvmFlag.CMS_INCREMENTAL_MODE)
            .add(JvmFlag.CMS_INCREMENTAL_PACING)
            .add(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY)
            .build();

    ImmutableMap<JvmFlag, JvmSearchSpace.GcMode> gcModeMap =
        ImmutableMap.<JvmFlag, JvmSearchSpace.GcMode>builder()
            .put(JvmFlag.USE_CONC_MARK_SWEEP_GC,
                JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP)
            .put(JvmFlag.USE_PARALLEL_GC, JvmSearchSpace.GcMode.USE_PARALLEL)
            .put(JvmFlag.USE_PARALLEL_OLD_GC, JvmSearchSpace.GcMode.USE_PARALLEL_OLD)
            .put(JvmFlag.USE_SERIAL_GC, JvmSearchSpace.GcMode.USE_SERIAL)
            .build();

    // all CommandLineArguments start at 0 and go to higher than 2 - this is a little brittle
    // though
    JvmSearchSpace.Int64Range intRange = Int64Range.newBuilder()
        .setFloor(1L)
        .setCeiling(2L)
        .setStepSize(1L)
        .build();

    for (JvmFlag missingArg : JvmFlag.values()) {
      // skip gc mode skip we need to omit them all
      if (gcModeMap.containsKey(missingArg)) {
        continue;
      }

      JvmSearchSpace.Builder searchSpaceBuilder = JvmSearchSpace.newBuilder();
      for (JvmFlag arg : JvmFlag.values()) {
        if (arg == missingArg) {
          continue;
        }

        if (gcModeMap.containsKey(arg)) {
          searchSpaceBuilder.addGcMode(gcModeMap.get(arg));
        } else if (boolCmdLineArguments.contains(arg)) {
          searchSpaceBuilder.setField(argToProtoFieldMap.get(arg), Boolean.TRUE);
        } else {
          searchSpaceBuilder.setField(argToProtoFieldMap.get(arg), intRange);
        }
      }
      try {
        ProtoBufSearchSpaceBundle bundle =
            new ProtoBufSearchSpaceBundle(searchSpaceBuilder.build(), true);
        fail("failed to throw for " + missingArg.name());
      } catch (InvalidConfigurationException expected) {
        // leaving out a parameter setting should generate an error
      }
    }
  }

}
