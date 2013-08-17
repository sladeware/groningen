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
import org.arbeitspferde.groningen.utility.Clock;
import org.joda.time.Instant;

import java.text.DateFormat;
import java.text.DecimalFormat;
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

  private final Clock clock;
  private final ExperimentDb experimentDb;
  private final PipelineId pipelineId;
  private final HistoryDatastore historyDatastore;
  private final PipelineManager pipelineManager;

  /** List of objects to be monitored */
  @VisibleForTesting final List<DisplayableObject> monitoredObjects =
    Collections.synchronizedList(new ArrayList<DisplayableObject>());

  /** Provides back links from monitored objects to their {@link Displayable}
   * wrapper so we can remove an object */
  private final Hashtable<Object, DisplayableObject> objectToDisplayable =
    new Hashtable<Object, DisplayableObject>();

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

  /** stores the current unique and merged evaluated subjects */
  @VisibleForTesting final List<EvaluatedSubject> currentEvaluatedSubjects =
    Collections.synchronizedList(new ArrayList<EvaluatedSubject>());

  /** stores the all-time unique and merged evaluated subjects */
  @VisibleForTesting final List<EvaluatedSubject> alltimeEvaluatedSubjects =
    Collections.synchronizedList(new ArrayList<EvaluatedSubject>());

  /** stores a list of warnings for display to the user */
  @VisibleForTesting final List<String> warnings =
      Collections.synchronizedList(new ArrayList<String>());

  private DisplayClusters displayableClusters;

  @Inject
  public DisplayMediator (final Clock clock, final ExperimentDb experimentDb,
      final HistoryDatastore historyDatastore, final PipelineManager pipelineManager,
      final PipelineId pipelineId) {
    this.clock = clock;
    this.experimentDb = experimentDb;
    this.pipelineId = pipelineId;
    this.historyDatastore = historyDatastore;
    this.pipelineManager = pipelineManager;
  }

  /**
   * Given a {@link List} of {@link EvaluatedSubject}, it detects duplicates
   * and returns a Hashtable of unique subjects. By unique, we mean ones which
   * have different {@link CommandLine#toArgumentString()}.
   *
   * @param targetList the {@link List} containing duplicates
   * @return a hash table of lists. Keys are the {@link CommandLine#toArgumentString()},
   *         pointing to a list of duplicated {@link EvaluatedSubject}.
   */
  private Hashtable<String, List<EvaluatedSubject>> detectDuplicates
    (List<EvaluatedSubject> targetList) {
    // Note that Hashtable is synchronized
    Hashtable<String, List<EvaluatedSubject>> uniqueSubjects =
      new Hashtable<String, List<EvaluatedSubject>>();
    // put each subject commandline in hashtable
    synchronized (targetList) {
      for (EvaluatedSubject evaluatedSubject : targetList) {
        // TODO(team): Fix Law of Demeter violations here.
        String commandLine = evaluatedSubject.getBridge().getCommandLine().toArgumentString();
          if (uniqueSubjects.containsKey(commandLine)) { // duplicate
            uniqueSubjects.get(commandLine).add(evaluatedSubject);
          } else { // first occurrence
            List<EvaluatedSubject> firstOccurrence =
              Collections.synchronizedList(new ArrayList<EvaluatedSubject>());
            firstOccurrence.add(evaluatedSubject);
            uniqueSubjects.put(commandLine, firstOccurrence);
          }
      }
    }
    return uniqueSubjects;
  }

  /**
   * Merges the unique items of the current run together. It merges duplicates
   * by taking their average score. For example, three instances of the same
   * subject scoring 21, 23 and 29 on the most recent run, will have value:
   * (21 + 23 + 29) / 3.
   *
   * @param targetList the current run {@link List} to be merged
   */
  private void cleanRecentRun(List<EvaluatedSubject> targetList) {
    Hashtable<String, List<EvaluatedSubject>> uniqueSubjects =
      detectDuplicates(targetList);

    synchronized (targetList) {
      targetList.clear(); //reset and repopulate
      final long experimentId = getExperimentId();
      for (List<EvaluatedSubject> duplicates : uniqueSubjects.values()) {
        if (duplicates.size() > 1) {
          double fitness = 0;
          for (EvaluatedSubject duplicate : duplicates) {
            fitness += duplicate.getFitness();
          }
          fitness /= duplicates.size();
          targetList.add(new EvaluatedSubject(clock, duplicates.get(0).getBridge(),
            fitness, experimentId));
        } else { // if just one subject
          targetList.add(duplicates.get(0));
        }
      }
    }
  }

  /**
   * Merges the unique items of the current run with the unique items of older
   * runs. It removes any duplicates. It weighs each score by its generation
   * number, and updates all scores. For example, a subject scoring 21 on run 1,
   * didn't appear in run 2, and 19 on run 3, will have value: 21 * 1 + 19 * 3.
   *
   * @param oldUniqueItemsList a {@link List} of all previous unique items.
   * @param newUniqueItemsList a {@link List} of current run unique items
   */
  private void mergeWeightedSumFitness(List<EvaluatedSubject> oldUniqueItemsList,
                                       List<EvaluatedSubject> newUniqueItemsList) {
    // list is already unique, put it in hash
    Hashtable<String, List<EvaluatedSubject>> uniqueNewSubjects =
       detectDuplicates(newUniqueItemsList);

    synchronized (oldUniqueItemsList) {
      double currentScore;
      final long experimentId = getExperimentId();

      Preconditions.checkState(experimentId > 0, "experimentId (%s) <= 0; data loss ensues.",
          experimentId);

      for (EvaluatedSubject evaluatedSubject : oldUniqueItemsList) {
        currentScore = 0;
        final String key = evaluatedSubject.getBridge().getCommandLine().toArgumentString();
        // if old subject occurs in current experiment
        if (uniqueNewSubjects.containsKey(key)) {
          currentScore = uniqueNewSubjects.get(key).get(0).getFitness();
          uniqueNewSubjects.remove(key);
          // update the experiment the evaluated subject is associated with.
          evaluatedSubject.setExperimentId(experimentId);
        }
        evaluatedSubject.setFitness(evaluatedSubject.getFitness() +
          currentScore * experimentId);
      }
      // add remaining new entries
      for (List<EvaluatedSubject> newItem : uniqueNewSubjects.values()) {
        synchronized (newItem) {
          oldUniqueItemsList.add(new EvaluatedSubject(clock, newItem.get(0).getBridge(),
            newItem.get(0).getFitness() * experimentId, experimentId));
        }
      }
    }
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
    assert(pipeline != null);
    
    /* First detect and remove duplicates in the temp list
     * Take the average of duplicates in the most recent run */
    cleanRecentRun(tempEvaluatedSubjects);

    /* Populate the current subject list */
    currentEvaluatedSubjects.clear();
    currentEvaluatedSubjects.addAll(tempEvaluatedSubjects);
    Collections.sort(currentEvaluatedSubjects, Collections.reverseOrder());

    /* Merge, detect and remove duplicates in the alltime list */
    mergeWeightedSumFitness(alltimeEvaluatedSubjects, tempEvaluatedSubjects);
    Collections.sort(alltimeEvaluatedSubjects, Collections.reverseOrder());

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

    /*
     * TODO(team): Please give special attention to this, as I think the old code could be
     * predicated on a dangerous assumption.
     */
    evaluatedSubject.setExperimentId(getExperimentId());
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
   * Wraps the object to be monitored in a {@link Displayable} and add to list
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
              individual.getBridge().getAssociatedSubject().getSubjectGroup().getClusterName());
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
    synchronized (alltimeEvaluatedSubjects) {
      return alltimeEvaluatedSubjects.toArray(new EvaluatedSubject[0]);
    }
  }

  public DisplayableObject[] getMonitoredObjects() {
    return monitoredObjects.toArray(new DisplayableObject[0]);
  }
}
