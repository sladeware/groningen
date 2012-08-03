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
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

/**
 * A repository of {@link Formatter} implementations.
 *
 * {@link Formatter} would have been defined as private static interface of
 * {@link JvmFlag} and these implementations as private static classes
 * contained therein; except that the Java language specification disallows
 * enums to access internal static fields in constructors before they are
 * initialized, but the enum elements must be initialized first.
 */
class Formatters {
  /**
   * Utility class; no instantiation allowed!
   */
  private Formatters() {
  }

  /**
   * Provide a representation and validations for integer-based JVM Flags.
   */
  static final Formatter INTEGER_FORMATTER = new Formatter() {
    /**
     * Validate that numeric values are in the acceptable range for {@link Integer}.
     */
    private final Range<Long> inherentAcceptableValues = Ranges.closed((long) Integer.MIN_VALUE,
      (long) Integer.MAX_VALUE);

    @Override
    public String asArgumentString(final JvmFlag cla, final long value) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      return String.format("%s%s%s%s%s", cla.getHotSpotFlagType().getPrefix(), cla.getName(),
          cla.getValueSeparator().getInfix(), value, cla.getDataSize().getSuffix());
    }

    @Override
    public String asRegularExpressionString(final JvmFlag cla) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      return String.format("%s%s%s\\d+%s\\b", cla.getHotSpotFlagType().getPrefix(), cla.getName(),
        cla.getValueSeparator().getInfix(), cla.getDataSize().unitFamilyAsRegexpString());
    }

    @Override
    public String asAcceptableValuesString(final JvmFlag cla) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      return cla.getAcceptableValueRange().toString();
    }

    @Override
    public void validate(final JvmFlag cla, final long proposedValue)
        throws IllegalArgumentException {
      Preconditions.checkNotNull(cla, "cla may not be null.");
      Preconditions.checkArgument(inherentAcceptableValues.contains(proposedValue));
      Preconditions.checkArgument(
          cla.getAcceptableValueRange().contains(proposedValue),
          "The flag %s with range %s cannot contain proposed value %s.",
          cla.getName(), cla.getAcceptableValueRange(),
          proposedValue);
    }
  };

  /**
   * Provide a representation and validations for boolean-based JVM Flags.
   */
  static final Formatter BOOLEAN_FORMATTER = new Formatter() {
    private final long TRUE_AS_LONG = 1;

    private final Range<Long> inherentAcceptableValues = Ranges.closed(0L, 1L);

    @Override
    public String asArgumentString(final JvmFlag cla,
                                   final long value) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      final String plusOrMinus = value == TRUE_AS_LONG ? "+" : "-";

      return String.format("%s%s%s", cla.getHotSpotFlagType().getPrefix(), plusOrMinus,
        cla.getName());
    }

    @Override
    public String asRegularExpressionString(final JvmFlag cla) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      return String.format("%s[+-]%s", cla.getHotSpotFlagType().getPrefix(), cla.getName());
    }

    @Override
    public String asAcceptableValuesString(final JvmFlag cla) {
      Preconditions.checkNotNull(cla, "cla may not be null.");

      return "{0 (false), 1 (true)}";
    }

    @Override
    public void validate(final JvmFlag cla, final long proposedValue)
        throws IllegalArgumentException {
      Preconditions.checkNotNull(cla, "cla may not be null.");
      Preconditions.checkArgument(inherentAcceptableValues.contains(proposedValue));
    }
  };

}
