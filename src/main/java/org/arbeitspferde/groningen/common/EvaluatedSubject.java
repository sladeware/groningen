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

package org.arbeitspferde.groningen.common;

import com.google.common.base.Preconditions;

import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.utility.Clock;

import org.joda.time.Instant;

/*
 * TODO(team): Evaluate whether this class can be made immutable and produced via a factory that
 *             usefully links it to an experiment.
 */

/**
 * Holder class for a subject, its fitness and time stamps.
 * Natural order for this class is increasing fitness values.
 */
public class EvaluatedSubject implements Comparable<EvaluatedSubject> {
  private static final long EXPERIMENT_ID_UNINITIALIZED = 0;
  private final SubjectStateBridge bridge;
  private final Clock clock;
  private Instant timeStamp;
  private final Fitness fitness = new Fitness();

  private long experimentId = EXPERIMENT_ID_UNINITIALIZED;
  private boolean isDefault = false;
  private String clusterName;
  private String subjectGroupName;
  private String userName;
  private int subjectGroupIndex;


  /** Constructors */
  public EvaluatedSubject(final Clock clock, final SubjectStateBridge subject, final double score,
      final long experimentId) {
    this(clock, subject, score);
    setExperimentId(experimentId);
  }

  public EvaluatedSubject(final Clock clock, final SubjectStateBridge bridge, final double score) {
    this.clock = clock;
    this.bridge = bridge;
    setFitness(score);
    setTimeStamp(this.clock.now());

    if (bridge.getAssociatedSubject() != null) {
      setClusterName(bridge.getAssociatedSubject().getGroup().getClusterName());
      setSubjectGroupName(bridge.getAssociatedSubject().getGroup().getName());
      setUserName(bridge.getAssociatedSubject().getGroup().getUserName());
      setSubjectGroupIndex(bridge.getAssociatedSubject().getIndex());
      setDefault(bridge.getAssociatedSubject().isDefault());
    }
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setSubjectGroupName(String subjectGroupName) {
    this.subjectGroupName = subjectGroupName;
  }

  public String getSubjectGroupName() {
    return subjectGroupName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserName() {
    return userName;
  }

  public void setSubjectGroupIndex(int subjectGroupIndex) {
    this.subjectGroupIndex = subjectGroupIndex;
  }

  public int getSubjectGroupIndex() {
    return subjectGroupIndex;
  }

  /** Set and get methods */
  public SubjectStateBridge getBridge() {
    return bridge;
  }

  public void setFitness(double score) {
    this.fitness.setFitness(score);
  }

  public double getFitness() {
    return fitness.getFitness();
  }

  public void setExperimentId(final long experimentId) throws IllegalArgumentException {
    Preconditions.checkArgument(experimentId > EXPERIMENT_ID_UNINITIALIZED,
        "Invalid experimentId: %s; should have been > %s.", experimentId,
        EXPERIMENT_ID_UNINITIALIZED);

    this.experimentId = experimentId;
  }

  public long getExperimentId() {
    Preconditions.checkState(experimentId != EXPERIMENT_ID_UNINITIALIZED,
        "experimentID is unset.");
    return experimentId;
  }

  public void setTimeStamp(Instant timeStamp) {
    this.timeStamp = timeStamp;
  }

  public Instant getTimeStamp() {
    return timeStamp;
  }

  /** Uses natural order to compare */
  @Override
  public int compareTo(EvaluatedSubject evaluatedSubject) {
    return Double.compare(fitness.getFitness(), evaluatedSubject.getFitness());
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean trueOrFalse) {
    isDefault = trueOrFalse;
  }

  /**
   * A class for Fitness, to be extended later.
   */
  private class Fitness {
    private double fitnessScore;

    public double getFitness() {
      return fitnessScore;
    }

    public void setFitness(double fitness) {
      this.fitnessScore = fitness;
    }
  }
}
