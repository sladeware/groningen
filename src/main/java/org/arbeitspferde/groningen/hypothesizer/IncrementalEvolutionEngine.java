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

package org.arbeitspferde.groningen.hypothesizer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.SystemClock;

import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionUtils;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class provides incremental version of GA, where the calling code has control over the
 * generations. We cannot use the default {@link GenerationalEvolutionEngine} for Groningen since
 * the fitness is computed after the experiment is run on the servers. {@link
 * GenerationalEvolutionEngine} will run the generations until the stopping conditions are met.
 * Instead, this class evolves one generation at a time and returns the population back.
 *
 * This class is not thread safe.
 *
 * @param <T> the type of elements this evolution engine handles.
 */
public class IncrementalEvolutionEngine<T> extends GenerationalEvolutionEngine<T> {

  private final Set<EvolutionObserver<T>> observers =
      new CopyOnWriteArraySet<>();

  private final CandidateFactory<T> candidateFactory;
  private final EvolutionaryOperator<T> evolutionScheme;
  private final FitnessEvaluator<? super T> fitnessEvaluator;
  private final SelectionStrategy<? super T> selectionStrategy;
  private final Random rng;
  private final int populationSize;
  private final int eliteCount;
  private final TerminationCondition[] conditions;
  private final Clock clock;

  private int currentGenerationIndex;
  private long startTime;
  private boolean initialized;
  private boolean terminated;

  public IncrementalEvolutionEngine(
      CandidateFactory<T> candidateFactory,
      EvolutionaryOperator<T> evolutionScheme,
      FitnessEvaluator<? super T> fitnessEvaluator,
      SelectionStrategy<? super T> selectionStrategy,
      Random rng,
      int populationSize,
      int eliteCount,
      TerminationCondition... conditions) {
    this(
        candidateFactory,
        evolutionScheme,
        fitnessEvaluator,
        selectionStrategy,
        rng,
        populationSize,
        eliteCount,
        new SystemClock(),
        conditions);
  }

  @VisibleForTesting IncrementalEvolutionEngine(
      CandidateFactory<T> candidateFactory,
      EvolutionaryOperator<T> evolutionScheme,
      FitnessEvaluator<? super T> fitnessEvaluator,
      SelectionStrategy<? super T> selectionStrategy,
      Random rng,
      int populationSize,
      int eliteCount,
      Clock clock,
      TerminationCondition... conditions) {
    super(candidateFactory, evolutionScheme, fitnessEvaluator, selectionStrategy, rng);
    Preconditions.checkArgument(
        eliteCount < populationSize, "Elite count has to be smaller that the population size.");

    this.candidateFactory = candidateFactory;
    this.evolutionScheme = evolutionScheme;
    this.fitnessEvaluator = fitnessEvaluator;
    this.selectionStrategy = selectionStrategy;
    this.rng = rng;
    this.populationSize = populationSize;
    this.eliteCount = eliteCount;
    this.conditions = conditions;
    this.clock = clock;
    super.setSingleThreaded(true);
  }

  /**
   * Performs the first step in the GA, which is to create the initial population by using {@link
   * CandidateFactory}. The population is returned to the caller. This method can be called only
   * once, and before calling {#performNextStep()} method.
   *
   * @return Initial population.
   */
  public List<T> performFirstStep() {
    if (initialized) {
      throw new IllegalStateException("Evolutionary process has already started.");
    }
    if (terminated) {
      throw new IllegalStateException("Evolutionary process has been terminated already.");
    }

    startTime = clock.now().getMillis();

    List<T> population =
        candidateFactory.generateInitialPopulation(populationSize, Collections.<T>emptySet(), rng);
    initialized = true;

    return population;
  }

  /**
   * Performs the subsequent steps in the GA, which is to evolve the population one generation at a
   * time. Unlike other evolution engines, here the population is passed in by the calling code.
   * This method computes the fitness of each individual in the population, applies the evolutionary
   * operators on the population, and returns the population. Call {#performFirstStep()} method
   * before calling this method.
   *
   * @param population Current population.
   * @return Evolved population.
   */
  public List<T> performNextStep(List<T> population) {
    if (!initialized) {
      throw new IllegalStateException("Evolutionary process has not been started yet.");
    }
    if (terminated) {
      throw new IllegalStateException("Evolutionary process has been terminated already.");
    }
    Preconditions.checkArgument(
        population != null && population.size() == populationSize, "Invalid population.");

    // Calculate the fitness scores for each member of the population.
    List<EvaluatedCandidate<T>> evaluatedPopulation = evaluatePopulation(population);

    // Sort the individuals according to their fitness. Individual with highest fitness value will
    // be at the top of the population.
    EvolutionUtils.sortEvaluatedPopulation(evaluatedPopulation, fitnessEvaluator.isNatural());

    // Generate population data
    PopulationData<T> data = EvolutionUtils.getPopulationData(
        evaluatedPopulation, fitnessEvaluator.isNatural(), 0, currentGenerationIndex, startTime);

    notifyPopulationChange(data, evaluatedPopulation);

    // Check if we have met the termination conditions. Only check when there are conditions.
    List<TerminationCondition> satisfiedConditions = null;
    if (conditions[0] != null) {
      satisfiedConditions = EvolutionUtils.shouldContinue(data, conditions);
    }

    // If not met, evolve the population by one generation, and return the evolved population.
    if (satisfiedConditions == null) {
      ++currentGenerationIndex;
      population = performEvolution(evaluatedPopulation, eliteCount, rng);
    } else {
      terminated = true;
    }

    // Otherwise, just return the current population
    return population;
  }

  /**
   * Returns whether the GA has met the termination conditions. If terminated, calling code should
   * not call {#performFirstStep()} or {#performNextStep()} method any more.
   *
   * @return Whether the GA has met the termination conditions or not.
   */
  public boolean isTerminated() {
    return terminated;
  }

  /**
   * Performs single evolution.
   */
  private List<T> performEvolution(
      List<EvaluatedCandidate<T>> evaluatedPopulation, int eliteCount, Random rng) {
    List<T> population = Lists.newArrayListWithCapacity(evaluatedPopulation.size());

    // First perform any elitist selection.
    List<T> elite = Lists.newArrayListWithCapacity(eliteCount);
    Iterator<EvaluatedCandidate<T>> iterator = evaluatedPopulation.iterator();
    while (elite.size() < eliteCount) {
      elite.add(iterator.next().getCandidate());
    }

    // Then select candidates that will be operated on to create the evolved
    // portion of the next generation.
    population.addAll(
        selectionStrategy.select(evaluatedPopulation, fitnessEvaluator.isNatural(),
            evaluatedPopulation.size() - eliteCount, rng));

    // Then evolve the population.
    population = evolutionScheme.apply(population, rng);

    // When the evolution is finished, add the elite individuals to the population.
    population.addAll(elite);

    return population;
  }

  /**
   * Adds a listener to receive status updates on the evolution progress. Updates are dispatched
   * synchronously on the request thread. Observers should complete their processing and return in a
   * timely manner to avoid holding up the evolution.
   *
   * @param observer An evolution observer call-back.
   */
  public void addEvolutionObserver(EvolutionObserver<T> observer) {
    observers.add(observer);
  }

  /**
   * Send the population data to all registered observers.
   *
   * @param data Information about the current state of the population.
   */
  private void notifyPopulationChange(
      PopulationData<T> data, List<EvaluatedCandidate<T>> population) {
    for (EvolutionObserver<T> observer : observers) {
      observer.populationUpdate(data, population);
    }
  }
}
