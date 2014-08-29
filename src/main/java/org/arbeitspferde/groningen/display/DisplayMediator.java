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

package org.arbeitspferde.groningen.display;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.HistoryDatastore;
import org.arbeitspferde.groningen.HistoryDatastore.HistoryDatastoreException;
import org.arbeitspferde.groningen.Pipeline;
import org.arbeitspferde.groningen.PipelineHistoryState;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineManager;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.scorer.HistoricalBestPerformerScorer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Encapsulates the Groningen information to be displayed on the HUD Implements a
 * Mediator pattern.
 */
@PipelineScoped
public class DisplayMediator implements Displayable, MonitorGroningen {
  private static final Logger log = Logger.getLogger(DisplayMediator.class.getCanonicalName());

  private static final Joiner commaJoiner = Joiner.on(",");

  /** Time keeping */
  private final DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

  private final ExperimentDb experimentDb;
  private final PipelineId pipelineId;
  private final HistoryDatastore historyDatastore;
  private final PipelineManager pipelineManager;

  /** List of objects to be monitored */
  @VisibleForTesting final List<DisplayableObject> monitoredObjects =
    Collections.synchronizedList(new ArrayList<DisplayableObject>());

  /** Provides back links from monitored objects to their {@link DisplayableObject}
   * wrapper so we can remove an object */
  private final Hashtable<Object, DisplayableObject> objectToDisplayable =
    new Hashtable<>();

  /** The separator for printing the monitored objects */
  String separator = "";

  /** The maximum number of individuals we care about in each list */
  @VisibleForTesting int maxIndv = 3;

  private long cummulativeExperimentIdSum = 0;

  /*
   * TODO(team): Look closely at the use of the List<EvaluatedSubject> below to ensure that they
   *             do not populate ad infinitum.
   */

  /** stores the evaluated subjects while still being added */
  @VisibleForTesting final List<EvaluatedSubject> tempEvaluatedSubjects =
    Collections.synchronizedList(new ArrayList<EvaluatedSubject>());

  /**
   * stores the current unique and merged evaluated subjects.
   *
   * we synchronize access to the list (and combine clearing and re-adding elements in
   * steps that should be atomic) so the list itself does not need internal locking.
   */
  @VisibleForTesting final List<EvaluatedSubject> currentEvaluatedSubjects =
      new ArrayList<>();

  /** stores a list of warnings for display to the user */
  @VisibleForTesting final List<String> warnings =
      Collections.synchronizedList(new ArrayList<String>());

  /** best performer scorer and store */
  final HistoricalBestPerformerScorer bestPerformerScorer;

  private DisplayClusters displayableClusters;

  // TODO(team): remove reference to clock
  @Inject
  public DisplayMediator(final ExperimentDb experimentDb,
      final HistoryDatastore historyDatastore, final PipelineManager pipelineManager,
      final PipelineId pipelineId, final HistoricalBestPerformerScorer bestPerformerScorer) {
    this.experimentDb = experimentDb;
    this.pipelineId = pipelineId;
    this.historyDatastore = historyDatastore;
    this.pipelineManager = pipelineManager;
    this.bestPerformerScorer = bestPerformerScorer;
  }

  /**
   * Closes out accounting on a generation and starts accounting on a new
   * generation.
   */
  @Override
  public void processGeneration() {
    cummulativeExperimentIdSum += getExperimentId();

    this.stopMonitoringObject(displayableClusters);
    displayableClusters = new DisplayClusters(tempEvaluatedSubjects);
    this.monitorObject(displayableClusters, "Clusters used in the last experiment");

    Pipeline pipeline = pipelineManager.findPipelineById(pipelineId);
    // TODO(team): propagate error up more gracefully than via an assert.
    assert(pipeline != null);

    List<EvaluatedSubject> cleanedCurGeneration =
        bestPerformerScorer.addGeneration(tempEvaluatedSubjects);
    synchronized (currentEvaluatedSubjects) {
      currentEvaluatedSubjects.clear();
      if (cleanedCurGeneration != null) {
        currentEvaluatedSubjects.addAll(cleanedCurGeneration);
      }
    }

    /* Reset temporary list */
    tempEvaluatedSubjects.clear();
  }

  /**
   * Adds an {@link EvaluatedSubject}.
   *
   * @param evaluatedSubject the {@link EvaluatedSubject} to add
   */
  @Override
  public void addIndividual(final EvaluatedSubject evaluatedSubject) {
    Preconditions.checkNotNull(evaluatedSubject, "evaluatedSubject may not be null.");
    tempEvaluatedSubjects.add(evaluatedSubject);
  }

  /** Adds a warning to the warning list */
  @Override
  public void addWarning(String warning) {
    Preconditions.checkNotNull(warning);
    synchronized (warnings) {
      if (!warnings.contains(warning)) {
        warnings.add(warning);
      }
    }
  }

  /**
   * Determines the maximum number of distinct individuals we care about
   *
   * @param max an integer
   */
  @Override
  public void maxIndividuals(int max) {
    this.maxIndv = max;
  }

  /**
   * Wraps the object to be monitored in a {@link DisplayableObject} and add to list
   * of monitored objects. The objects should be thread safe.
   *
   * @param obj
   * @param infoString
   */
  @Override
  public void monitorObject(Object obj, String infoString) {
    DisplayableObject newAddition = new DisplayableObject(obj, infoString);
    objectToDisplayable.put(obj, newAddition);
    monitoredObjects.add(newAddition);
  }

  /**
   * Given a monitored {@link Object}, the class stops monitoring it. It returns
   * {@code true} if the object was being monitored. It returns false otherwise.
   *
   * @param obj
   */
  @Override
  public boolean stopMonitoringObject(Object obj) {
    synchronized (monitoredObjects) {
      // return false if object not initialized
      if (obj == null) {
        return false;
      }
      return monitoredObjects.remove(objectToDisplayable.remove(obj));
    }
  }

  /**
   * Calls the .toHtml() method of each registered {@link Displayable} object
   *
   * @return a concatenated html string
   */
  @Deprecated
  @Override
  public String toHtml() {
    // TODO(sanragsood): Remove this method entirely once the cleanup is complete.
    return "Deprecated";
  }

  private class DisplayClusters {
    private final String toDisplay;

    private DisplayClusters(List<EvaluatedSubject> evalSubjects) {
      Set<String> clusters = Collections.synchronizedSet(new HashSet<String>());
      synchronized (evalSubjects) {
        for (EvaluatedSubject individual : evalSubjects) {
          // skip if no subject, which is the case in unitTesting
          if (individual.getBridge().getAssociatedSubject() == null) {
            continue;
          }
          clusters.add(
              individual.getBridge().getAssociatedSubject().getGroup().getClusterName());
        }
      }
      toDisplay = commaJoiner.join(clusters);
    }

    @Override
    public String toString() {
      return toDisplay;
    }
  }

  /**
   * A provider of the current {@link ExperimentDb#getExperimentId()} to lessen the Law of Demeter
   * smells.
   *
   * @return The current experiment ID.
   */
  private long getExperimentId() {
    return experimentDb.getExperimentId();
  }

  /**
   * Returns warnings generated for the pipeline.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }

  public long getCumulativeExperimentIdSum() {
    return cummulativeExperimentIdSum;
  }

  public EvaluatedSubject[] getCurrentExperimentSubjects() {
    synchronized (currentEvaluatedSubjects) {
      return currentEvaluatedSubjects.toArray(new EvaluatedSubject[0]);
    }
  }

  public EvaluatedSubject[] getAllExperimentSubjects() {
    List<EvaluatedSubject> allSubjects = Lists.newArrayList();
    List<PipelineHistoryState> states = Lists.newArrayList();
    try {
      states = historyDatastore.getStatesForPipelineId(pipelineId);
    } catch (HistoryDatastoreException e) {
      log.severe(e.getMessage());
    }
    for (PipelineHistoryState state : states) {
      allSubjects.addAll(Lists.newArrayList(state.evaluatedSubjects()));
    }
    return allSubjects.toArray(new EvaluatedSubject[] {});
  }

  public EvaluatedSubject[] getAlltimeExperimentSubjects() {
      return bestPerformerScorer.getBestPerformers().toArray(new EvaluatedSubject[0]);
  }

  public DisplayableObject[] getMonitoredObjects() {
    return monitoredObjects.toArray(new DisplayableObject[0]);
  }
}
