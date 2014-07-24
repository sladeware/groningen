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

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * An immutable class to represent a set of {@link JvmFlag}.
 *
 * It provides, among other things, semantic validation of the state of JVM flags at-large
 * throughout the system.
 */
public class JvmFlagSet {
  private static final Logger log = Logger.getLogger(JvmFlagSet.class.getCanonicalName());

  private final Map<JvmFlag, Long> finalValues;

  /**
   * Non-instantiatiable; please use {@link Builder}.
   */
  private JvmFlagSet(final Map<JvmFlag, Long> values) {
    finalValues = new TreeMap<JvmFlag, Long>(Preconditions.checkNotNull(
        values, "values may not be null."));
  }

  /**
   * Retrieve the value associated with a given {@link JvmFlag}.
   *
   * @param flag The flag name.
   * @return The value.
   */
  public long getValue(final JvmFlag flag) {
    Preconditions.checkNotNull(flag, "flag may not be null.");

    final Long value = finalValues.get(flag);
    Preconditions.checkNotNull(value, "value should not be null: %s", flag);
    return value;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(JvmFlagSet.class)
        .addValue(finalValues)
        .toString();
  }

  /**
   * A mechanism for conveniently building {@link JvmFlagSet}.
   *
   */
  public static class Builder {
    private final Map<JvmFlag, Long> assignedValues = new EnumMap<JvmFlag, Long>(JvmFlag.class);
    private static final Map<JvmFlag, Long> defaultValues =
        new EnumMap<JvmFlag, Long>(JvmFlag.class);

    static {
      // TODO(team): Evaluate whether more sensible defaults should be set.

      /* TODO(team): Address this sagely wisdom from team member:
       * this seems like it needs to be documented.
       * It also seems like we want a hard and soft validation here. On that allows use
       * of the defaults and one that doesn't. I could be swayed away from this stance
       * though. I think we are losing some checks by moving from explicit args to
       * methods toward map based flag passing and I'd like for say the hypothesizer at
       * least to use the strict version so we don't miss locations where someone forgot
       * to add the read/write of the new flag.
       *
       * I don't think the defaults were used outside of test. So perhaps exposing an
       * @VFT allowDefaults()  method might give you the flexibility to make testing easy
       * without having us potentially lose flag values.
       */
      for (final JvmFlag flag : JvmFlag.values()) {
        defaultValues.put(flag, 0L);
      }
    }

    /**
     * Non-instantiable; please use {@link JvmFlagSet#builder()};
     */
    private Builder() {
    }

    /**
     * Assign a value to a given {@link JvmFlag}.
     *
     * @param flag The flag.
     * @param value The value.
     * @throws IllegalArgumentException in case a nonsensical value is provided.
     */
    public Builder withValue(final JvmFlag flag, final long value) throws IllegalArgumentException {
      Preconditions.checkNotNull(flag, "flag may not be null.");

      flag.validate(value);

      if (assignedValues.get(flag) != null) {
        log.warning(String.format(
            "Flag %s is being reset in value; this is probably not intended.", flag));
      }

      assignedValues.put(flag, value);

      return this;
    }

    /**
     * Yield the {@link JvmFlagSet} after performing validations and such.
     *
     * All unassigned values will receive a system-determined default value.
     *
     * @return The final {@link JvmFlagSet}.
     */
    public JvmFlagSet build() {
      final Map<JvmFlag, Long> emission = new EnumMap<JvmFlag, Long>(JvmFlag.class);

      for (final JvmFlag flag : defaultValues.keySet()) {
        final Long defaultValue = defaultValues.get(flag);
        final Long assignedValue = assignedValues.get(flag);

        final Long actualValue = Objects.firstNonNull(assignedValue, defaultValue);

        emission.put(flag, actualValue);
      }

      validate();
      return new JvmFlagSet(emission);
    }

    /**
     * Check the validity of all arguments.
     */
    private void validate()  {
      /*
       *  TODO(team): While migrating code to this, audit places for sanity/semantic checking and
       *             migrate tests to here.
       *
       *  TODO(team): Evaluate fixing invalid value conditions.
       */
      if (areAssigned(JvmFlag.HEAP_SIZE, JvmFlag.MAX_NEW_SIZE)) {
        final long heapMinimum = assignedValues.get(JvmFlag.HEAP_SIZE);
        final long maxNewSize = assignedValues.get(JvmFlag.MAX_NEW_SIZE);

        if (maxNewSize >= heapMinimum) {
          log.severe(String.format(
              "XXX FIXME XXX - heapMinimum %s should never be smaller than maxNewSize %s.",
              heapMinimum, maxNewSize));
        }
      }

      final List<JvmFlag> gcModes = ImmutableList.<JvmFlag>builder()
          .add(JvmFlag.USE_CONC_MARK_SWEEP_GC)
          .add(JvmFlag.USE_PARALLEL_GC)
          .add(JvmFlag.USE_PARALLEL_OLD_GC)
          .add(JvmFlag.USE_SERIAL_GC)
          .build();

      final List<JvmFlag> setModes = Lists.newArrayList();

      for (final JvmFlag flag : gcModes) {
        if (areAssigned(flag)) {
          final long value = assignedValues.get(flag);

          if (value > 0) {
            setModes.add(flag);
          }
        }
      }

      final int setModesCount = setModes.size();

      if (setModesCount == 0) {
        log.severe("XXX FIXME XXX - At least one GC mode must be set.");
      } else if (setModesCount > 1) {
        final String warning = new StringBuilder()
            .append("Only one GC mode may be set: ")
            .append(Joiner.on(" ").join(setModes))
            .append(".")
            .toString();
        log.severe(String.format("XXX FIXME XXX - %s", warning));
      }
    }

    /**
     * Check that the given {@link JvmFlag} are explicitly set.
     *
     * @param flags The flags to check.
     * @return True if all the enumerated flags were explicitly set.
     */
    private boolean areAssigned(final JvmFlag ...flags) {
      for (final JvmFlag flag : flags) {
        if (!assignedValues.containsKey(flag)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Produce a new {@link Builder}.
   *
   * @return The empty {@link Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }
}
