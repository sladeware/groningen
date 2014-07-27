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

import com.google.common.collect.Maps;

import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;

import java.util.EnumMap;
import java.util.List;

/**
 * Map a protobuf JvmSearchSpace to a GenericSearchSpaceBundle.
 */
public class ProtoBufSearchSpaceBundle extends GenericSearchSpaceBundle {
  static final EnumMap<ProgramConfiguration.JvmSearchSpace.GcMode, JvmFlag> gcModeMap =
      Maps.newEnumMap(ProgramConfiguration.JvmSearchSpace.GcMode.class);

  // initialize the enum mappings
  static {
    gcModeMap.put(ProgramConfiguration.JvmSearchSpace.GcMode.USE_CONC_MARK_SWEEP,
        JvmFlag.USE_CONC_MARK_SWEEP_GC);
    gcModeMap.put(ProgramConfiguration.JvmSearchSpace.GcMode.USE_PARALLEL, JvmFlag.USE_PARALLEL_GC);
    gcModeMap.put(ProgramConfiguration.JvmSearchSpace.GcMode.USE_PARALLEL_OLD,
        JvmFlag.USE_PARALLEL_OLD_GC);
    gcModeMap.put(ProgramConfiguration.JvmSearchSpace.GcMode.USE_SERIAL, JvmFlag.USE_SERIAL_GC);
  }

  /**
   * Map each of the command line arguments from the setting in the protobuf to the enum, throwing
   * exceptions if the user requests that all fields must be present (all translates to including at
   * least one of the gc modes).
   *
   * @param protoSpace the protocol buffer {@link ProgramConfiguration.JvmSearchSpace} from which we
   *     should create this {@link SearchSpaceBundle}
   * @param requireAll true iff the user wants an exception to be thrown for fields that are not
   *     specified.  If false and a field is not specified, the default range will be pulled from
   *     {@link JvmFlag}.
   * @throws InvalidConfigurationException a value or range was either not within the expected
   *     bounds or the complete set was requested but not specified in the protobuf.
   */
  public ProtoBufSearchSpaceBundle(final ProgramConfiguration.JvmSearchSpace protoSpace,
                                   final boolean requireAll) throws InvalidConfigurationException {

    // initial param check - the rest of the CommandLineArgs will be checked
    // their entry is instantiated
    if (protoSpace.getGcModeCount() == 0 && requireAll) {
      throw new InvalidConfigurationException("no gc mode specified");
    }

    /*
     * Run through all the different enums
     *
     * This is kind of a lot of boiler plate code with unnecessary calls to the get methods for
     * fields that were not defined in the protobuf.  It perhaps could be condensed via annotations
     * or reflection. The latter was avoided in favor of compile time verification of method names.
     * The former was avoided due to lack of experience by the implementor, but from my amount of
     * understanding, an annotation based method would still require use of reflection.
     *
     * TODO(team): if requireAll is true, we make the user pin values that are not going to be
     * included in the final command line.  It would be good if we can require only the values that
     * in 'mutually inclusive' sets so users don't have to pin values that have no bearing. But
     * that's annoying and time consuming for mach 1, and the workaround is not too large a burden.
     */
    makeInt64RangeEntry(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR,
        protoSpace.hasAdaptiveSizeDecrementScaleFactor(),
        protoSpace.getAdaptiveSizeDecrementScaleFactor(), requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_EXP_AVG_FACTOR, protoSpace.hasCmsExpAvgFactor(),
        protoSpace.getCmsExpAvgFactor(), requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE,
        protoSpace.hasCmsIncrementalDutyCycle(), protoSpace.getCmsIncrementalDutyCycle(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN,
        protoSpace.hasCmsIncrementalDutyCycleMin(), protoSpace.getCmsIncrementalDutyCycleMin(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_INCREMENTAL_OFFSET, protoSpace.hasCmsIncrementalOffset(),
        protoSpace.getCmsIncrementalOffset(), requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR,
        protoSpace.hasCmsIncrementalSafetyFactor(), protoSpace.getCmsIncrementalSafetyFactor(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION,
        protoSpace.hasCmsInitiatingOccupancyFraction(),
        protoSpace.getCmsInitiatingOccupancyFraction(), requireAll);

    makeInt64RangeEntry(JvmFlag.GC_TIME_RATIO, protoSpace.hasGcTimeRatio(),
        protoSpace.getGcTimeRatio(), requireAll);

    makeInt64RangeEntry(JvmFlag.MAX_GC_PAUSE_MILLIS, protoSpace.hasMaxGcPauseMillis(),
        protoSpace.getMaxGcPauseMillis(), requireAll);

    makeInt64RangeEntry(JvmFlag.MAX_HEAP_FREE_RATIO, protoSpace.hasMaxHeapFreeRatio(),
        protoSpace.getMaxHeapFreeRatio(), requireAll);

    makeInt64RangeEntry(JvmFlag.MIN_HEAP_FREE_RATIO, protoSpace.hasMinHeapFreeRatio(),
        protoSpace.getMinHeapFreeRatio(), requireAll);

    makeInt64RangeEntry(JvmFlag.NEW_RATIO, protoSpace.hasNewRatio(), protoSpace.getNewRatio(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.NEW_SIZE, protoSpace.hasNewSize(), protoSpace.getNewSize(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.MAX_NEW_SIZE, protoSpace.hasMaxNewSize(),
        protoSpace.getMaxNewSize(), requireAll);

    makeInt64RangeEntry(JvmFlag.PARALLEL_GC_THREADS, protoSpace.hasParallelGcThreads(),
        protoSpace.getParallelGcThreads(), requireAll);

    makeInt64RangeEntry(JvmFlag.SURVIVOR_RATIO, protoSpace.hasSurvivorRatio(),
        protoSpace.getSurvivorRatio(), requireAll);

    makeInt64RangeEntry(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT,
        protoSpace.hasTenuredGenerationSizeIncrement(),
        protoSpace.getTenuredGenerationSizeIncrement(), requireAll);

    makeInt64RangeEntry(JvmFlag.HEAP_SIZE, protoSpace.hasHeapSize(), protoSpace.getHeapSize(),
        requireAll);

    makeInt64RangeEntry(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT,
        protoSpace.hasYoungGenerationSizeIncrement(), protoSpace.getYoungGenerationSizeIncrement(),
        requireAll);

    makeBoolEntry(JvmFlag.CMS_INCREMENTAL_MODE, protoSpace.hasCmsIncrementalMode(),
        protoSpace.getCmsIncrementalMode(), requireAll);

    makeBoolEntry(JvmFlag.CMS_INCREMENTAL_PACING, protoSpace.hasCmsIncrementalPacing(),
        protoSpace.getCmsIncrementalPacing(), requireAll);

    makeBoolEntry(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY,
        protoSpace.hasUseCmsInitiatingOccupancyOnly(),
        protoSpace.getUseCmsInitiatingOccupancyOnly(), requireAll);

    makeInt64RangeEntry(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB,
        protoSpace.hasSoftRefLruPolicyMsPerMb(), protoSpace.getSoftRefLruPolicyMsPerMb(),
        requireAll);

    /*
     * Finally deal with GC Mode
     *
     * We have a repeated list here so that we can select multiple gc modes over which to search.
     * However, squashing these down to boolean values seems wrong.
     *
     * TODO(team): Really, we want to return a set of the available gc modes but need to
     * understand implications on hypothesizer and the GA in general to see if that makes sense.
     * Not for mach1.
     */
    if (protoSpace.getGcModeCount() == 0) {
      // use the 'full' (it is boolean) search step to look over each gcmode
      for (JvmFlag gcModeArg : JvmFlag.getGcModeArguments()) {
        makeBoolEntry(gcModeArg, false, false, false);
      }
    } else {
      /*
       * this list is puny so we can do contains() searches on it even if we have a dumb user who
       * puts lots of duplicates in their config....
       *
       * Essentially, we say we have a value to pin to - true if it is present, false otherwise.
       */
      List<ProgramConfiguration.JvmSearchSpace.GcMode> gcModes = protoSpace.getGcModeList();
      for (ProgramConfiguration.JvmSearchSpace.GcMode mode :
          ProgramConfiguration.JvmSearchSpace.GcMode.values()) {
        makeBoolEntry(gcModeMap.get(mode), true, gcModes.contains(mode), false);
      }
    }
  }

  /**
   * Small method to do verification on an Int64Range and make a
   * SearchSpaceEntry for it
   *
   * @param arg the {@link JvmFlag} with which this range/{@link SearchSpaceBundle.SearchSpaceEntry}
   *     shall be equated
   * @param has true iff the protobuf contained a value for this
   * @param range user described range
   * @param required if the field was not included, should an exception be thrown
   * @throws InvalidConfigurationException the range was inconsistent or missing when required
   */
  protected void makeInt64RangeEntry(final JvmFlag arg,
                                     final boolean has,
                                     final ProgramConfiguration.JvmSearchSpace.Int64Range range,
                                     final boolean required) throws InvalidConfigurationException {


    GenericSearchSpaceEntry entry = null;
    if (has) {
      // validation has been pushed to this function, so validate everything
      if (range.hasValue() && (range.hasFloor() || range.hasCeiling())) {
        throw new InvalidConfigurationException("had value and range setting for " + arg.name());
      } else if (range.hasValue()) {
        if (range.getValue() < arg.getMinimum() || range.getValue() > arg.getMaximum()) {
          StringBuilder sb = new StringBuilder();
          sb.append("value outside default range of ").append(arg.getMinimum()).append(":")
              .append(arg.getMaximum()).append(" for ").append(arg.name());
          throw new InvalidConfigurationException(sb.toString());
        }
        entry = new GenericSearchSpaceEntry(arg, range.getValue(), range.getValue(), 0L);
      } else if (range.hasFloor() && range.hasCeiling()) {
        if (range.getFloor() < arg.getMinimum() || range.getCeiling() > arg.getMaximum()) {
          StringBuilder sb = new StringBuilder();
          sb.append("specified range ").append(range.getFloor()).append(":")
              .append(range.getCeiling()).append(" has endpoint outside default range of ")
              .append(arg.getMinimum()).append(":").append(arg.getMaximum()).append(" for ")
              .append(arg.name());
          throw new InvalidConfigurationException(sb.toString());
        }
        // TODO(team): add default step size to JvmFlag and clean up hardcoded
        // value
        entry =
            new GenericSearchSpaceEntry(arg, range.getFloor(), range.getCeiling(),
            range.hasStepSize() ? range.getStepSize() : 1L);
      } else {
        throw new InvalidConfigurationException("incomplete range specified for " + arg.name());
      }
    } else if (required) {
      throw new InvalidConfigurationException("missing required Int64Range " + arg.name());
    } else {
      // TODO(team): add default step size to JvmFlag and clean up hardcoded
      // value
      entry = new GenericSearchSpaceEntry(arg, arg.getMinimum(), arg.getMaximum(),
          arg.getStepSize());
    }

    // finally the entry is ready to be stored
    entries[arg.ordinal()] = entry;

  }

  /**
   * Small method to do verification on an Int64Range and make a SearchSpaceEntry for it.
   *
   * @param arg the {@link JvmFlag} with which this
   *     range/{@link SearchSpaceBundle.SearchSpaceEntry} shall be equated.
   * @param has true iff the protobuf contained a value for this
   * @param truthVal which truth value the user selected if present
   * @param required if the field was not included, should an exception be thrown
   * @throws InvalidConfigurationException the range was inconsistent or missing when required
   */
  protected void makeBoolEntry(final JvmFlag arg,
                               final boolean has,
                               final boolean truthVal,
                               final boolean required) throws InvalidConfigurationException {

    GenericSearchSpaceEntry entry = null;
    if (has) {
      long val = (truthVal) ? 1L : 0L;
      entry = new GenericSearchSpaceEntry(arg, val, val, 0L);
    } else if (required) {
      throw new InvalidConfigurationException("missing required boolean " + arg.name());
    } else {
      // TODO(team): pull from JvmFlag when its back in
      entry = new GenericSearchSpaceEntry(arg, 0L, 1L, 1L);
    }

    // finally the entry is ready to be stored
    entries[arg.ordinal()] = entry;

  }
}
