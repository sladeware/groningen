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
 * A encapsulation of the bundle of search space parameters for each argument over which Groningen
 * can search.  The SearchSpaceEntry objects are tightly coupled with the elements of the
 * {@link JvmFlag} enum since the default ranges per argument are hardcoded within the enum, and
 * which will need to be retrieved from the enum should the user not specify a full range and step
 * size.  Entries define a range and possibly a step size. Each field is represented as a long
 * (booleans will take values 0 and 1).
 */
public interface SearchSpaceBundle {

  /**
   * Bean for a specific command line arg's search range
   */
  public interface SearchSpaceEntry {

    /**
     * The Bean will know which {@link JvmFlag} it represents
     *
     * @return {@link JvmFlag} this bean represents
     */
    public JvmFlag getRepresentedArg();

    /**
     * The Bean will know the floor of the range
     *
     * @return long representing the floor (inclusive) of the range
     */
    public long getFloor();

    /**
     * The Bean will know the ceiling of the range
     *
     * @return long representing the ceiling (inclusive) of the range
     */
    public long getCeiling();

    /**
     * The Bean will know how large of steps it should take when exploring the space
     *
     * @return long representing the step size
     */
    public long getStepSize();
  }

  /**
   * Retrieve the {@link SearchSpaceEntry} for a specific {@link JvmFlag}. The
   * {@link SearchSpaceEntry} is guaranteed to specify a range
   *
   * @param arg {@link JvmFlag} for which the {@link SearchSpaceEntry} is being requested.
   *
   * @return the associated entry
   */
  public SearchSpaceEntry getSearchSpace(final JvmFlag arg);

  /**
   * Retrieve the {@link SearchSpaceEntry SearchSpaceEntrys} for all
   * {@link JvmFlag CommandLineArguments}. Each {@link SearchSpaceEntry} is
   * guaranteed to specify a range.
   *
   * @return an array of {@link SearchSpaceEntry} indexed by the ordinal of the {@link JvmFlag} with
   *     which it is associated.
   */
  public SearchSpaceEntry[] getSearchSpaces();
}
