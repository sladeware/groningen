package org.arbeitspferde.groningen.scorer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.utility.Clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * HistoricalBestPerformerScorer that uses the generation number as a major weight in scoring.
 *
 * The individual's score for this iteration will be weighted (multiplied) by the iteration
 * number.
 *
 * The best performer scoring will combine individuals both within an experiment if they are
 * duplicated AND will combine scores across iterations. Namely, within the same generation
 * individuals with the same command line (args and arg values) will be averaged.
 *
 * Across iterations, the individual's previous score will be combined with to its score in this
 * iteration - the individual will be represented by a single @{link EvaluatedSubject} with the
 * current iteration number and a score equal to the combination of the previous plus current
 * scores.
 */
public class GenerationNumberWeightedBestPerformerScorer implements HistoricalBestPerformerScorer {

  /**
   * To create new instances of {@link EvaluatedSubject EvaluatedSubjects} when merging existing
   * instance in the store.
   */
  private final Clock clock;

  /** Stores the all-time unique and merged evaluated subjects with greatest to least ordering. */
  @VisibleForTesting
  final
  List<EvaluatedSubject> alltimeEvaluatedSubjects = new ArrayList<>();

  @Inject
  public GenerationNumberWeightedBestPerformerScorer(Clock clock) {
    this.clock = clock;
  }

  /** @see HistoricalBestPerformerScorer#getBestPerformers() */
  @Override
  public List<EvaluatedSubject> getBestPerformers() {
    synchronized (alltimeEvaluatedSubjects) {
      return new ArrayList<>(alltimeEvaluatedSubjects);
    }
  }

  /** @see HistoricalBestPerformerScorer#getBestPerformers(int) */
  @Override
  public List<EvaluatedSubject> getBestPerformers(int maxEntries) {
    synchronized (alltimeEvaluatedSubjects) {
      return new ArrayList<>(alltimeEvaluatedSubjects.subList(0, maxEntries));
    }
  }

  /** @see HistoricalBestPerformerScorer#addGeneration(List) */
  @Override
  public List<EvaluatedSubject> addGeneration(List<EvaluatedSubject> newGeneration) {

    /*
     * detect and remove duplicates in the list of individuals in this iteration.
     * duplicate the array so that we don't have to bother with locking as we process
     * the generation.
     */
    Map<String, EvaluatedSubject> cleanedLastIterationList =
        cleanRecentRun(new ArrayList<>(newGeneration));

    // Build the return value - the cleaned generation. Callee will own this copy.
    List<EvaluatedSubject> cleanedGenerationList =
        new ArrayList<>(cleanedLastIterationList.values());
    Collections.sort(cleanedGenerationList, Collections.reverseOrder());

    /* Merge, detect and remove duplicates in the alltime list */
    synchronized (alltimeEvaluatedSubjects) {
      mergeWeightedSumFitness(alltimeEvaluatedSubjects, cleanedLastIterationList);
      // TODO(team): consider some type of pruning to this tree.
      Collections.sort(alltimeEvaluatedSubjects, Collections.reverseOrder());
    }

    return cleanedGenerationList;
  }

  /**
   * Merges the unique items of the current run together. It merges duplicates
   * by taking their average score. For example, three instances of the same
   * subject scoring 21, 23 and 29 on the most recent run, will have value:
   * (21 + 23 + 29) / 3.
   *
   * @param targetList the {@link List} of the iteration's
   *        {@link EvaluatedSubject EvaluatedSubjects} to be cleaned. Will not modify the List.
   * @returns a {@link Map} of unique EvaluatedSubjects keyed by their command lines.
   */
  @VisibleForTesting
  Map<String, EvaluatedSubject> cleanRecentRun(List<EvaluatedSubject> targetList) {
    Map<String, List<EvaluatedSubject>> uniqueSubjects = detectDuplicates(targetList);

    Map<String, EvaluatedSubject> cleanedSubjectMap = new HashMap<>();

    for (Entry<String, List<EvaluatedSubject>> duplicateEntry : uniqueSubjects.entrySet()) {
      String commandLine = duplicateEntry.getKey();
      List<EvaluatedSubject> duplicates = duplicateEntry.getValue();
      if (duplicates.size() > 1) {
        double fitness = 0.0;
        for (EvaluatedSubject duplicate : duplicates) {
          fitness += duplicate.getFitness();
        }
        fitness /= duplicates.size();
        EvaluatedSubject firstDup = duplicates.get(0);
        long experimentId = firstDup.getExperimentId();
        cleanedSubjectMap.put(commandLine, new EvaluatedSubject(clock, firstDup.getBridge(),
            fitness, experimentId));
      } else { // if just one subject
        cleanedSubjectMap.put(commandLine, duplicates.get(0));
      }
    }

    return cleanedSubjectMap;
  }

  /**
   * Given a {@link List} of {@link EvaluatedSubject}, it detects duplicates
   * and returns a HashMap of unique subjects. By unique, we mean ones which
   * have different
   * {@link org.arbeitspferde.groningen.experimentdb.CommandLine#toArgumentString()
   * CommandLine#toArgumentString()}.
   *
   * @param targetList the {@link List} containing duplicates
   * @return a map of lists. Keys are the CommandLine.toArgumentString(),
   *         pointing to a list of duplicated {@link EvaluatedSubject}.
   */
  @VisibleForTesting
  Map<String, List<EvaluatedSubject>> detectDuplicates(List<EvaluatedSubject> targetList) {

    // Put all subjects in a HashMap
    HashMap<String, List<EvaluatedSubject>> uniqueSubjects =
      new HashMap<>();

    for (EvaluatedSubject evaluatedSubject : targetList) {
      // TODO(team): Fix Law of Demeter violations here.
      String commandLine = evaluatedSubject.getBridge().getCommandLine().toArgumentString();
      if (!uniqueSubjects.containsKey(commandLine)) {
        uniqueSubjects.put(commandLine, new ArrayList<EvaluatedSubject>());
      }
      uniqueSubjects.get(commandLine).add(evaluatedSubject);
    }

    return uniqueSubjects;
  }


  /**
   * Merges the unique items of the current run with the unique items of older
   * runs. It removes any duplicates. It weighs each score by its generation
   * number, and updates all scores. For example, a subject scoring 21 on run 1,
   * didn't appear in run 2, and 19 on run 3, will have value: 21 * 1 + 19 * 3.
   *
   * The resultant array does not get sorted here.
   *
   * @param oldUniqueItemsList a {@link List} of all previous unique items.
   * @param uniqueNewSubjects a {@link List} of current run unique items
   */
  private void mergeWeightedSumFitness(List<EvaluatedSubject> oldUniqueItemsList,
                                       Map<String, EvaluatedSubject> uniqueNewSubjects) {

    for (EvaluatedSubject evaluatedSubject : oldUniqueItemsList) {
      final String key = evaluatedSubject.getBridge().getCommandLine().toArgumentString();
      // if old subject occurs in current experiment
      if (uniqueNewSubjects.containsKey(key)) {
        EvaluatedSubject uniqueNewEvaledSubject = uniqueNewSubjects.get(key);
        double currentScore = uniqueNewSubjects.get(key).getFitness();
        long experimentId = uniqueNewEvaledSubject.getExperimentId();
        uniqueNewSubjects.remove(key);
        // update the experiment the evaluated subject is associated with.
        evaluatedSubject.setExperimentId(experimentId);
        evaluatedSubject.setFitness(evaluatedSubject.getFitness() +
            currentScore * experimentId);
      }
    }
    // add remaining new entries
    for (EvaluatedSubject newItem : uniqueNewSubjects.values()) {
      long experimentId = newItem.getExperimentId();
      oldUniqueItemsList.add(new EvaluatedSubject(clock, newItem.getBridge(),
          newItem.getFitness() * experimentId, experimentId));
    }
  }
}
