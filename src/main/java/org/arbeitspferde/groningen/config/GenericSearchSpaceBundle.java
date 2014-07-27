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

import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;

/**
 * Abstract implementation of SearchSpaceBundle laying out the general framework for retrieving
 * immutable SearchSpaceEntry beans.
 *
 * Instantiation of the set of SearchSpaceEntries seems directly tied to the method of
 * configuration, and as such, for values other than the default ranges for each argument, this task
 * which has been left to the subclasses to override in the constructor.
 */
public class GenericSearchSpaceBundle implements SearchSpaceBundle {

  protected final GenericSearchSpaceEntry[] entries =
      new GenericSearchSpaceEntry[JvmFlag.values().length];

  /**
   * Instantiate with an array of the default values. Assumes we have not overridden the default
   * ordinal values in the enum.
   */
  public GenericSearchSpaceBundle() {
    for (final JvmFlag arg : JvmFlag.values()) {
      // TODO(team): incorporate default step size per arg definition from its source once the
      // authoritative source for said definition is decided and implemented
      entries[arg.ordinal()] =
          new GenericSearchSpaceEntry(arg, arg.getMinimum(), arg.getMaximum(), 1);
    }
  }

  /**
   * @see SearchSpaceBundle#getSearchSpace(JvmFlag)
   */
  @Override
  public SearchSpaceEntry getSearchSpace(final JvmFlag arg) {
    return entries[arg.ordinal()];
  }

  /**
   * @see SearchSpaceBundle#getSearchSpaces()
   */
  @Override
  public SearchSpaceEntry[] getSearchSpaces() {
    return entries.clone();
  }

  /**
   * Basic bean implementation of SearchSpaceEntry in which all members are marked final
   */
  public static class GenericSearchSpaceEntry implements SearchSpaceEntry {

    final JvmFlag represents;
    final long floor;
    final long ceiling;
    final long step;

    public GenericSearchSpaceEntry(final JvmFlag represents, final long floor, final long ceiling,
        final long step) {
      this.represents = represents;
      this.floor = floor;
      this.ceiling = ceiling;
      this.step = step;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getRepresentedArg()
     */
    @Override
    public JvmFlag getRepresentedArg() {
      return represents;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getFloor()
     */
    @Override
    public long getFloor() {
      return floor;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getCeiling()
     */
    @Override
    public long getCeiling() {
      return ceiling;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getStepSize()
     */
    @Override
    public long getStepSize() {
      return step;
    }

  }
}
