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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;

import org.arbeitspferde.groningen.proto.GroningenConfigProto;

/**
 * An enumeration of the JVM flags that Groningen can manage.
 *
 * This class does not perform a lot of work itself but delegates much of it to
 * a variant of the flyweight design pattern with {@link Formatters} and
 * {@link Formatter}.  The documentation contained therein notes why these
 * facets are not defined as attributes of this class itself.
 */
public enum JvmFlag {
  // We are using Integer.MAX_VALUE - 1 as the max value (inclusive). The
  // Hypothesizer adds one to
  // this value and generates a random value from 0 inclusive to
  // Integer.MAX_VALUE exclusive.

  // TODO(team): Look at better ways of implementing this.
  HEAP_SIZE("<special-never-should-be-exposed>", HotSpotFlagType.NON_STANDARD, 1L, 64 * 1024L, 1L,
    DataSize.MEGA, ValueSeparator.NONE),

  ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR("AdaptiveSizeDecrementScaleFactor",
      HotSpotFlagType.UNSTABLE, 0L, 100L, 1L, DataSize.NONE, ValueSeparator.EQUAL),

  CMS_EXP_AVG_FACTOR("CMSExpAvgFactor", HotSpotFlagType.UNSTABLE, 0L, 100L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  CMS_INCREMENTAL_DUTY_CYCLE("CMSIncrementalDutyCycle", HotSpotFlagType.UNSTABLE, 0L, 100L, 1L,
      DataSize.NONE, ValueSeparator.EQUAL),

  CMS_INCREMENTAL_DUTY_CYCLE_MIN("CMSIncrementalDutyCycleMin", HotSpotFlagType.UNSTABLE, 0L, 100L,
      1L, DataSize.NONE, ValueSeparator.EQUAL),

  CMS_INCREMENTAL_OFFSET("CMSIncrementalOffset", HotSpotFlagType.UNSTABLE, 0L, 100L, 1L,
      DataSize.NONE, ValueSeparator.EQUAL),

  CMS_INCREMENTAL_SAFETY_FACTOR("CMSIncrementalSafetyFactor", HotSpotFlagType.UNSTABLE, 0L, 100L,
      1L, DataSize.NONE, ValueSeparator.EQUAL),

  CMS_INITIATING_OCCUPANCY_FRACTION("CMSInitiatingOccupancyFraction", HotSpotFlagType.UNSTABLE, 0L,
      100L, 1L, DataSize.NONE, ValueSeparator.EQUAL),

  GC_TIME_RATIO("GCTimeRatio", HotSpotFlagType.UNSTABLE, 0L, 1000L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  MAX_GC_PAUSE_MILLIS("MaxGCPauseMillis", HotSpotFlagType.UNSTABLE, 0L, 1000000L, 1L,
      DataSize.NONE, ValueSeparator.EQUAL),

  MAX_HEAP_FREE_RATIO("MaxHeapFreeRatio", HotSpotFlagType.UNSTABLE, 0L, 100L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  MAX_NEW_SIZE("MaxNewSize", HotSpotFlagType.UNSTABLE, 0L, 32 * 1024L, 16L, DataSize.MEGA,
      ValueSeparator.EQUAL),

  MIN_HEAP_FREE_RATIO("MinHeapFreeRatio", HotSpotFlagType.UNSTABLE, 0L, 100L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  NEW_RATIO("NewRatio", HotSpotFlagType.UNSTABLE, 0L, 1000L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  NEW_SIZE("NewSize", HotSpotFlagType.UNSTABLE, 0L, 32 * 1024L, 16L, DataSize.MEGA,
      ValueSeparator.EQUAL),

  PARALLEL_GC_THREADS("ParallelGCThreads", HotSpotFlagType.UNSTABLE, 0L, 100000L, 10L,
      DataSize.NONE, ValueSeparator.EQUAL),

  SURVIVOR_RATIO("SurvivorRatio", HotSpotFlagType.UNSTABLE, 0L, 1000L, 1L, DataSize.NONE,
      ValueSeparator.EQUAL),

  SOFT_REF_LRU_POLICY_MS_PER_MB("SoftRefLRUPolicyMSPerMB", HotSpotFlagType.UNSTABLE, 0L, 1000000L,
      1000L, DataSize.NONE, ValueSeparator.EQUAL),

  TENURED_GENERATION_SIZE_INCREMENT("TenuredGenerationSizeIncrement", HotSpotFlagType.UNSTABLE, 0L,
      100L, 1L, DataSize.NONE, ValueSeparator.EQUAL),

  YOUNG_GENERATION_SIZE_INCREMENT("YoungGenerationSizeIncrement", HotSpotFlagType.UNSTABLE, 0L,
      100L, 1L, DataSize.NONE, ValueSeparator.EQUAL),

  CMS_INCREMENTAL_MODE("CMSIncrementalMode", HotSpotFlagType.UNSTABLE),

  CMS_INCREMENTAL_PACING("CMSIncrementalPacing", HotSpotFlagType.UNSTABLE),

  USE_CMS_INITIATING_OCCUPANCY_ONLY("UseCMSInitiatingOccupancyOnly", HotSpotFlagType.UNSTABLE),

  USE_CONC_MARK_SWEEP_GC("UseConcMarkSweepGC", HotSpotFlagType.UNSTABLE),

  USE_PARALLEL_GC("UseParallelGC", HotSpotFlagType.UNSTABLE),

  USE_PARALLEL_OLD_GC("UseParallelOldGC", HotSpotFlagType.UNSTABLE),

  USE_SERIAL_GC("UseSerialGC", HotSpotFlagType.UNSTABLE);

  /**
   * The human-readable flag name that excludes the flag-specific argument
   * prefix, infix, suffix, and respective assignment value.
   */
  private final String name;

  /**
   * The type of flag that the flag is.
   */
  private final HotSpotFlagType hotSpotFlagType;

  /**
   * The {@link Formatter} flyweight.
   */
  private final Formatter formatter;

  /**
   * The minimum acceptable value for the flag.
   */
  private final Long floorValue;

  /**
   * The maximum acceptable value for the flag.
   */
  private final Long ceilingValue;

  /**
   * The size of the increment.
   */
  private final Long stepSize;

  /**
   * The associated scalar data size associated with this flag.
   */
  private final DataSize dataSize;

  /**
   * The type of infix delimiter between flag name and proposed value.
   */
  private final ValueSeparator valueSeparator;

  /**
   * The range of values allowed.
   */
  private final Range<Long> acceptableValueRange;

  private static final ImmutableSortedSet<JvmFlag> GC_MODE_FLAGS = ImmutableSortedSet.<JvmFlag>naturalOrder()
      .add(USE_CONC_MARK_SWEEP_GC)
      .add(USE_PARALLEL_GC)
      .add(USE_PARALLEL_OLD_GC)
      .add(USE_SERIAL_GC)
      .build();

  /**
   * Construct a new boolean flag.
   *
   * @param name The name of the flag.
   * @param hotSpotFlagType The type of the flag.
   */
  private JvmFlag(final String name, final HotSpotFlagType hotSpotFlagType) {
    this.name = Preconditions.checkNotNull(name, "name may not be null.");
    this.hotSpotFlagType = Preconditions.checkNotNull(hotSpotFlagType, "hotSpotFlagType may not be null.");

    formatter = Formatters.BOOLEAN_FORMATTER;
    floorValue = 0L;
    ceilingValue = 1L;
    stepSize = 1L;
    dataSize = DataSize.NONE;
    valueSeparator = ValueSeparator.NONE;
    acceptableValueRange = Range.closed(0L, 1L);
  }

  /**
   * Construct a new integer flag.
   *
   * @param name The name of the flag.
   * @param hotSpotFlagType The type of the flag.
   * @param minimum The minimum value.
   * @param maximum The maximum value.
   * @param stepSize The increment between values.
   */
  JvmFlag(final String name, final HotSpotFlagType hotSpotFlagType, final Long minimum,
      final Long maximum, final Long stepSize, final DataSize dataSize,
      final ValueSeparator valueSeparator) {
    this.name = Preconditions.checkNotNull(name, "name may not be null.");
    this.hotSpotFlagType = Preconditions.checkNotNull(hotSpotFlagType, "hotSpotFlagType may not be null.");
    floorValue = Preconditions.checkNotNull(minimum, "minimum may not be null.");
    ceilingValue = Preconditions.checkNotNull(maximum, "maximum may not be null.");
    this.stepSize = Preconditions.checkNotNull(stepSize, "stepSize may not be null.");
    this.dataSize = Preconditions.checkNotNull(dataSize, "dataSize may not be null.");
    this.valueSeparator = Preconditions.checkNotNull(valueSeparator, "valueSeparator may not be null.");

    formatter = Formatters.INTEGER_FORMATTER;
    acceptableValueRange = Range.closed(minimum, maximum);
  }

   String getName() {
    return name;
  }

  HotSpotFlagType getHotSpotFlagType() {
    return hotSpotFlagType;
  }

  Formatter getFormatter() {
    return formatter;
  }

  public long getMinimum() {
    return floorValue;
  }

  public long getMaximum() {
    return ceilingValue;
  }

  public long getStepSize() {
    return stepSize;
  }

  ValueSeparator getValueSeparator() {
    return valueSeparator;
  }

  DataSize getDataSize() {
    return dataSize;
  }

  Range<Long> getAcceptableValueRange() {
    return acceptableValueRange;
  }

  /**
   * Provide a representation of this flag with value as a String.
   *
   * @param value The value that should be represented in string form.
   * @return The String representation.
   */
  public String asArgumentString(final Long value) {
    return getFormatter().asArgumentString(this, value);
  }

  /**
   * Provide a representation of along with argument values as String.
   *
   * @return The String representation.
   */
  public String asRegularExpressionString() {
    return getFormatter().asRegularExpressionString(this);
  }

  /**
   * Provide a representation of the allowed values as String.
   *
   * @return The String representation.
   */
  public String asAcceptableValuesString() {
    return getFormatter().asAcceptableValuesString(this);
  }

  /**
   * Validate this flag with the proposed value.
   *
   * @param proposedValue The proposed value to be checked.
   * @throws IllegalArgumentException In case of a bad argument.
   */
  public void validate(final Long proposedValue) throws IllegalArgumentException {
    getFormatter().validate(this, proposedValue);
  }

  /**
   * Returns the GC mode {@link JvmFlag} instances.
   *
   * @return List of GC modes.
   */
  public static ImmutableSortedSet<JvmFlag> getGcModeArguments() {
    return GC_MODE_FLAGS;
  }

  /**
   * Returns the GC mode command-line argument for the given GC mode.
   */
  public static JvmFlag getGcModeArgument(final GcMode gcMode) {
    Preconditions.checkNotNull(gcMode, "gcMode should not be null.");

    switch (gcMode) {
      case CMS:
        return USE_CONC_MARK_SWEEP_GC;
      case PARALLEL:
        return USE_PARALLEL_GC;
      case PARALLEL_OLD:
        return USE_PARALLEL_OLD_GC;
      case SERIAL:
        return USE_SERIAL_GC;
      default:
        throw new IllegalArgumentException("Invalid GC mode: " + gcMode);
    }
  }

  public static JvmFlag getGcModeArgument(final GroningenConfigProto.ProgramConfiguration.JvmSearchSpace.GcMode mode) {
    Preconditions.checkNotNull(mode, "mode should not be null.");
    // initialize the enum mappings
    switch (mode) {
      case USE_CONC_MARK_SWEEP:
        return USE_CONC_MARK_SWEEP_GC;
      case USE_PARALLEL:
        return USE_PARALLEL_GC;
      case USE_PARALLEL_OLD:
        return USE_PARALLEL_OLD_GC;
      case USE_SERIAL:
        return USE_SERIAL_GC;
      default:
        throw new IllegalArgumentException("Invalid GC mode: " + mode);
    }
  }
}
