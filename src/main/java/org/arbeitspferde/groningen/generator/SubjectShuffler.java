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

package org.arbeitspferde.groningen.generator;

import com.google.inject.Inject;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.NamedConfigParam;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.subject.ServingAddressGenerator;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.subject.SubjectManipulator;
import org.arbeitspferde.groningen.utility.PermanentFailure;
import org.arbeitspferde.groningen.utility.TemporaryFailure;
import org.uncommons.maths.random.MersenneTwisterRNG;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Walks through a list of experimental{@link Subject}s that we can then associate with
 * {@link SubjectStateBridge} objects in the {@link Generator}.
 *
 * The order of the {@link List} of {@link Subject}s is randomized to minimize experimental bias.
 */
@PipelineIterationScoped
public class SubjectShuffler {
  /** Logger for this class */
  private static final Logger log =
      Logger.getLogger(SubjectShuffler.class.getCanonicalName());

  /**
   * Iterator class for list of subjects that also provides precached subjectsCount value
   */
  public static class SubjectIterator implements Iterator<Subject> {
    private final Iterator<Subject> subjectIterator;
    /**
     * Cache the total number of subjects in the shuffler so it can be queried without walking the
     * iterator
     */
    private final int subjectCount;

    public SubjectIterator(final Iterator<Subject> subjectIterator, final int subjectCount) {
      this.subjectIterator = subjectIterator;
      this.subjectCount = subjectCount;
    }

    @Override
    public boolean hasNext() {
      return subjectIterator.hasNext();
    }

    @Override
    public Subject next() {
      return subjectIterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    public int getSubjectCount() {
      return subjectCount;
    }
  }

  @Inject
  @NamedConfigParam("subject_group_number_of_shuffles")
  private int shuffleCount = GroningenParams.getDefaultInstance().getSubjectGroupNumberOfShuffles();

  /** The random number generator we are using */
  private final Random rng = new MersenneTwisterRNG();

  private final GroningenConfig config;
  private final SubjectManipulator manipulator;
  private final ServingAddressGenerator servingAddressBuilder;

  @Inject
  public SubjectShuffler(final GroningenConfig config, final SubjectManipulator manipulator,
      final ServingAddressGenerator servingAddressBuilder) {
    this.config = config;
    this.manipulator = manipulator;
    this.servingAddressBuilder = servingAddressBuilder;
  }

  /**
   * Initialize the list of {@link Subject}s by querying {@link GroningenConfig} and interacting
   * with the cluster environment by using the {@link SubjectManipulator} class.
   */
  public SubjectIterator createIterator() {
    List<Subject> subjects = null;
    for (final ClusterConfig clusterConfig : config.getClusterConfigs()) {
      final String clusterName = clusterConfig.getName();
      for (SubjectGroupConfig groupConfig : clusterConfig.getSubjectGroupConfigs()) {
        final String groupName = groupConfig.getName();
        final String userName = groupConfig.getUser();

        /*
         *  TODO(team): Allow this to be provided via Guice's Assisted Inject or factory to ease
         *              testing.
         */
        final SubjectGroup subjectGroup =
            new SubjectGroup(clusterName, groupName, userName, groupConfig, servingAddressBuilder);
        try {
          subjects = subjectGroup.initialize(manipulator, subjects);
        } catch (final TemporaryFailure e) {
          throw new RuntimeException("Could not create a list of subjects.", e);
        } catch (final PermanentFailure e) {
          throw new RuntimeException("Could not create a list of subjects.", e);
        }
      }
    }

    if (subjects != null) {
      if (subjects.size() > 1) {
        // We are shuffling multiple times to ensure a good quality shuffle. This may be overkill
        // because we're using a good quality random number generator. However, we only do this
        // once per experiment and the shuffle runs in linear time on a relatively small list. So,
        // why not shuffle a multiple times?
        for (int i = 0; i < shuffleCount; i++) {
          Collections.shuffle(subjects, rng);
        }
      }

      return new SubjectIterator(subjects.iterator(), subjects.size());
    } else {
      log.log(Level.SEVERE,
          "Empty iterator intialized; this should never happen!", new Throwable());
      throw new IllegalStateException("Empty iterator intialized; this should never happen!");
    }
  }
}
