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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.config.SearchSpaceBundle;
import org.arbeitspferde.groningen.config.SearchSpaceBundle.SearchSpaceEntry;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.profiling.ProfilingRunnable;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.Metric;
import org.arbeitspferde.groningen.utility.MetricExporter;

import org.uncommons.maths.number.ConstantGenerator;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.operators.ListCrossover;
import org.uncommons.watchmaker.framework.selection.TournamentSelection;
import org.uncommons.watchmaker.framework.termination.Stagnation;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Hypothesizer processes Validator and Extractor results stored in the
 * ExperDB to produce a hypothesis of what might improve the JVM memory
 * utilization, pause frequency and pause duration. The output are JVM setting
 * modifications stored in the ExperDB.
 */
@PipelineScoped
public class Hypothesizer extends ProfilingRunnable {

  /** Contains all non-GC mode arguments */
  @VisibleForTesting
  static final List<JvmFlag> ARGUMENTS;

  // Store non-GC mode arguments in ARGUMENTS
  static {
    Set<JvmFlag> gcModes = Sets.newHashSet(JvmFlag.getGcModeArguments());
    ARGUMENTS = Lists.newArrayList();
    for (JvmFlag argument : JvmFlag.values()) {
      if (!gcModes.contains(argument)) {
        ARGUMENTS.add(argument);
      }
    }
  }

  // Chromosome size is the number of arguments plus the GC mode.
  @VisibleForTesting
  static final int CHROMOSOME_SIZE = ARGUMENTS.size() + 1;

  /** Logger for this class */
  private static final Logger logger = Logger.getLogger(Hypothesizer.class.getCanonicalName());

  /** The Experimental Database */
  private final ExperimentDb experimentDb;
  private final MetricExporter metricExporter;

  private GroningenConfig config;

  private final List<JvmFlag> supportedGcModes = Lists.newArrayList();

  /**
   * population size is fixed once we instantiate the ga engine so it makes
   * sense to store it locally and provide a mutator by which to update it once.
   * Initial value is set to allow verification that the population size has
   * been set.
   */
  private final AtomicLong populationSize = new AtomicLong();

  /** Track the total fitness score for all members of the last generation */
  private final AtomicDouble totalFitnessScore = new AtomicDouble(0.0);

  private IncrementalEvolutionEngine<List<Integer>> gaEngine;

  private boolean initialized = false;

  private boolean notComplete = true;

  @Inject
  public Hypothesizer(final Clock clock, final MonitorGroningen monitor, final ExperimentDb e,
      final MetricExporter metricExporter) {
    super(clock, monitor);

    experimentDb = e;
    this.metricExporter = metricExporter;
  }

  /** Returns true when the Hypothesizer is not complete with experimentation. */
  public boolean notComplete() {
    return notComplete;
  }

  /**
   * Basic mutator for the population size with which to instantiate the ga
   * engine.
   *
   * @param size number in the population for the ga engine. Valid populations
   *        are ints > 0
   */
  public void setPopulationSize(final int size) {
    Preconditions.checkArgument(size > 0, "population must be greater than 0");
    populationSize.set(size);
  }

  @Override
  public void profiledRun(GroningenConfig config) {
    if (!notComplete) {
      throw new IllegalStateException("Hypothesizer cycle is complete.");
    }
    this.config = config;

    List<List<Integer>> population = null;
    if (initialized) {
      if (gaEngine == null) {
        logger.severe("Subsequent Hypothesizer invocation still has GA engine not initialized.");
        throw new IllegalStateException(
            "Subsequent Hypothesizer invocation still has GA engine not initialized.");
      }

      List<List<Integer>> currentPopulation = null;
      currentPopulation = loadPopulation();
      population = gaEngine.performNextStep(currentPopulation);
    } else {
      logger.log(Level.INFO, "First invocation of Hypothesizer.");

      initialize();

      // Create the initial population using the evolution engine.
      population = gaEngine.performFirstStep();

      initialized = true;
    }

    savePopulation(population);

    if (gaEngine.isTerminated()) {
      logger.info("Hypothesizer has reached termination conditions.");
      notComplete = false;
    }
  }

  /* Initializes the Hypothesizer */
  private void initialize() {
    if (populationSize.get() <= 0) {
      throw new IllegalStateException(String.format(
          "Hypothesizer initialized with invalid population size of %s.  Please verify that " +
          "population size has been set before starting Groningen.", populationSize));
    }

    Experiment lastExperiment = null;

    lastExperiment = experimentDb.getLastExperiment();

    if (lastExperiment != null) {
      logger.info("Hypothesizer is starting from the checkpoint.");
      List<Long> subjectIds = lastExperiment.getSubjectIds();
      if (subjectIds.size() != populationSize.get()) {
        throw new IllegalStateException(
            "Number of subjects in the last saved experiment from checkpoint file is not same "
                + "as the supplied population size. Please verify that correct population "
                + "size has been set before starting Groningen.");
      }
    } else {
      logger.info("Hypothesizer could not load previous experiment from "
          + "checkpoint; starting from scratch ...");
    }

    initializeSupportedGcModes();

    initializeEvolutionEngine(lastExperiment);
  }

  private void initializeSupportedGcModes() {
    List<JvmFlag> gcModes = JvmFlag.getGcModeArguments();

    SearchSpaceBundle bundle = config.getJvmSearchSpaceRestriction();
    for (JvmFlag gcMode : gcModes) {
      SearchSpaceEntry entry = bundle.getSearchSpace(gcMode);
      if (entry.getCeiling() > 0) {
        supportedGcModes.add(gcMode);
      }
    }

    if (supportedGcModes.isEmpty()) {
      throw new RuntimeException("No GC mode specified in the config.");
    }
    logger.info(String.format("Allowed GC modes: %s", supportedGcModes));
  }

  private void initializeEvolutionEngine(Experiment lastExperiment) {
    if (gaEngine != null) {
      logger.severe("First Hypothesizer invocation already has GA engine initialized.");
      throw new IllegalStateException(
          "First Hypothesizer invocation already has GA engine initialized.");
    }

    // Create a candidate factory that the GA framework will call to create the initial population.
    CandidateFactory<List<Integer>> candidateFactory =
        new CommandLineArgumentFactory(lastExperiment);

    // Set up the crossover and mutation operators.
    List<EvolutionaryOperator<List<Integer>>> operators = Lists.newArrayList();
    operators.add(new ListCrossover<Integer>(config.getParamBlock().getNumCrossovers()));
    operators.add(
      new IntegerListMutator(new Probability(config.getParamBlock().getMutationProb())));

    // Add the operators to the pipeline.
    EvolutionaryOperator<List<Integer>> pipeline = new EvolutionPipeline<>(operators);

    // Set up the fitness evaluator.
    FitnessEvaluator<List<Integer>> evaluator =
        new ListFitnessEvaluator((int) populationSize.get());

    // We use simple stagnation condition for terminating the GA.  If the population doesn't improve
    // over a certain number of evolutions, the GA stops.
    TerminationCondition condition = null;

    // Our only termination condition is stagnation and when that is defaulting
    // to 0 we ignore it
    if (config.getParamBlock().getStagnantGens() > 0) {
      condition = new Stagnation(config.getParamBlock().getStagnantGens(), evaluator.isNatural());
    }

    // Create an evolution engine with the above parameters.
    gaEngine =
        new IncrementalEvolutionEngine<>(candidateFactory, pipeline, evaluator,
            new TournamentSelection(new Probability(0.75)), new MersenneTwisterRNG(),
            (int) populationSize.get(), config.getParamBlock().getEliteCount(), condition);

    gaEngine.addEvolutionObserver(new EvolutionObserver<List<Integer>>() {
      @Override
      public void populationUpdate(PopulationData<List<Integer>> data,
          List<EvaluatedCandidate<List<Integer>>> population) {
        logger.info("************************************************************");
        logger.info(String.format("Generation %s: Best fitness %s",
            data.getGenerationNumber() + 1, data.getBestCandidateFitness()));
        logger.info(String.format("Best candidate: %s", data.getBestCandidate()));
        logger.info(String.format("Standard deviation: %s; Mean: %s",
            data.getFitnessStandardDeviation(), data.getMeanFitness()));
        logger.info("------------------------------------------------------------");
        for (int i = 0; i < population.size(); ++i) {
          EvaluatedCandidate<List<Integer>> candidate = population.get(i);

          logger.info(String.format("Candidate %s: %s; Fitness %s",
              i, candidate.getCandidate(), candidate.getFitness()));
        }
        logger.log(Level.INFO, "************************************************************");
      }
    });
  }

  /**
   * Loads the population for the given experiment from the command-line data
   * stored in ExperDB.
   */
  private List<List<Integer>> loadPopulation() {
    Experiment lastExperiment = experimentDb.getLastExperiment();
    logger.log(Level.INFO, String.format("Last experiment ID: %s", lastExperiment.getIdOfObject()));
    logger.log(Level.INFO,
        String.format("Last experiment subjects: %s", lastExperiment.getSubjectIds()));

    // Look-up the Subject IDs mapped to that experiment.
    List<Long> subjectIds = lastExperiment.getSubjectIds();

    List<List<Integer>> population = Lists.newArrayList();
    for (SubjectStateBridge subject : lastExperiment.getSubjects()) {
      // Load and save the individual from its command line.
      population.add(loadIndividual(subject.getCommandLine()));
    }
    return population;
  }

  /**
   * Creates an individual from the command-line parameters. The GC mode
   * argument is converted to an enum.
   */
  private List<Integer> loadIndividual(CommandLine commandLine) {
    List<Integer> individual = Lists.newArrayList();

    // Add the GC mode argument value as the first object.
    individual.add(supportedGcModes.indexOf(JvmFlag.getGcModeArgument(commandLine.getGcMode())));

    // Add the rest of the non-GC mode argument values.
    for (JvmFlag argument : ARGUMENTS) {
      long value = commandLine.getValue(argument);
      if (value > Integer.MAX_VALUE) {
        logger.log(Level.SEVERE, String.format("Value %s is larger than integer max for %s.",
            value, argument));
        throw new RuntimeException("Command-line argument value is larger than integer max.");
      }
      individual.add((int) value);
    }

    return individual;
  }

  /**
   * Updates experiment db with current generation subjects.
   *
   * @Return The new experiment for that population,.
   */
  private Experiment savePopulation(List<List<Integer>> population) {
    Preconditions.checkArgument(population != null && population.size() > 0, "Invalid population.");

    List<Long> subjectIds = Lists.newArrayListWithExpectedSize(population.size());
    for (List<Integer> individual : population) {
      SubjectStateBridge subject;

      subject = experimentDb.makeSubject();

      subjectIds.add(subject.getIdOfObject());

      final JvmFlagSet.Builder builder = JvmFlagSet.builder();

      // First gene is the GC mode.
      switch (supportedGcModes.get(individual.get(0))) {
        case USE_CONC_MARK_SWEEP_GC:
          builder.withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, 1L);
          break;
        case USE_PARALLEL_GC:
          builder.withValue(JvmFlag.USE_PARALLEL_GC, 1L);
          break;
        case USE_PARALLEL_OLD_GC:
          builder.withValue(JvmFlag.USE_PARALLEL_GC, 1L);
          break;
        case USE_SERIAL_GC:
          builder.withValue(JvmFlag.USE_SERIAL_GC, 1L);
          break;
        default:
          throw new RuntimeException("Invalid GC mode.");
      }

      for (int i = 1; i < individual.size(); ++i) {
        final int value = individual.get(i);

        switch (ARGUMENTS.get(i - 1)) {

          case ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR:
            builder.withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, value);
            break;
          case CMS_EXP_AVG_FACTOR:
            builder.withValue(JvmFlag.CMS_EXP_AVG_FACTOR, value);
            break;
          case CMS_INCREMENTAL_DUTY_CYCLE:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, value);
            break;
          case CMS_INCREMENTAL_DUTY_CYCLE_MIN:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, value);
            break;
          case CMS_INCREMENTAL_OFFSET:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, value);
            break;
          case CMS_INCREMENTAL_SAFETY_FACTOR:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, value);
            break;
          case CMS_INITIATING_OCCUPANCY_FRACTION:
            builder.withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, value);
            break;
          case GC_TIME_RATIO:
            builder.withValue(JvmFlag.GC_TIME_RATIO, value);
            break;
          case HEAP_SIZE:
            builder.withValue(JvmFlag.HEAP_SIZE, value);
            break;
          case MAX_GC_PAUSE_MILLIS:
            builder.withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, value);
            break;
          case MAX_HEAP_FREE_RATIO:
            builder.withValue(JvmFlag.MAX_HEAP_FREE_RATIO, value);
            break;
          case MAX_NEW_SIZE:
            builder.withValue(JvmFlag.MAX_NEW_SIZE, value);
            break;
          case MIN_HEAP_FREE_RATIO:
            builder.withValue(JvmFlag.MIN_HEAP_FREE_RATIO, value);
            break;
          case NEW_RATIO:
            builder.withValue(JvmFlag.NEW_RATIO, value);
            break;
          case NEW_SIZE:
            builder.withValue(JvmFlag.NEW_SIZE, value);
            break;
          case PARALLEL_GC_THREADS:
            builder.withValue(JvmFlag.PARALLEL_GC_THREADS, value);
            break;
          case SURVIVOR_RATIO:
            builder.withValue(JvmFlag.SURVIVOR_RATIO, value);
            break;
          case TENURED_GENERATION_SIZE_INCREMENT:
            builder.withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, value);
            break;
          case YOUNG_GENERATION_SIZE_INCREMENT:
            builder.withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, value);
            break;
          case SOFT_REF_LRU_POLICY_MS_PER_MB:
            builder.withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, value);
            break;
          case CMS_INCREMENTAL_MODE:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_MODE, value);
            break;
          case CMS_INCREMENTAL_PACING:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_PACING, value);
            break;
          case USE_CMS_INITIATING_OCCUPANCY_ONLY:
            builder.withValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY, value);
            break;
          default:
            throw new RuntimeException("Invalid command-line argument.");
        }
      }

      final JvmFlagSet jvmFlagSet = builder.build();

      subject.storeCommandLine(jvmFlagSet);
    }

    // Make and cache a new experiment with the new subject IDs.
    Experiment experiment = experimentDb.makeExperiment(subjectIds);
    return experiment;
  }

  /**
   * Class to generate the initial population.
   */
  private class CommandLineArgumentFactory extends AbstractCandidateFactory<List<Integer>> {
    private final List<SubjectStateBridge> subjects;
    private int index = 0;

    CommandLineArgumentFactory(Experiment checkpointExperiment) {
      if (checkpointExperiment == null) {
        subjects = Collections.emptyList();
      } else {
        subjects = checkpointExperiment.getSubjects();
      }
    }

    @Override
    public List<Integer> generateRandomCandidate(final Random rng) {
      if (!subjects.isEmpty()) {
        // Starting from checkpoint experiment. Return the current subject's
        // command line.
        return loadIndividual(subjects.get(index++).getCommandLine());
      }
      final List<Integer> individual = Lists.newArrayListWithExpectedSize(CHROMOSOME_SIZE);

      // Pick a GC mode at random, and add it as the first object.
      individual.add(rng.nextInt(supportedGcModes.size()));

      final SearchSpaceBundle bundle = config.getJvmSearchSpaceRestriction();

      // Add values for the rest of the arguments.
      for (final JvmFlag argument : ARGUMENTS) {
        final SearchSpaceEntry entry = bundle.getSearchSpace(argument);
        final int value = generateRandomNumber(entry, rng);
        logger.info(String.format("Search space for %s: floor=%s; ceiling=%s; step=%s; value=%s",
            argument, entry.getFloor(), entry.getCeiling(), entry.getStepSize(), value));
        individual.add(value);
      }
      return individual;
    }
  }

  /** Generates a random number based on the search space entry */
  private static int generateRandomNumber(SearchSpaceEntry entry, Random rng) {
    Preconditions.checkArgument(entry != null, "Search space cannot be null.");
    Preconditions.checkArgument(entry.getFloor() >= 0, "Floor value cannot be negative.");
    Preconditions.checkArgument(
      entry.getCeiling() < Integer.MAX_VALUE, "Ceiling value cannot be greater than integer max.");
    Preconditions.checkArgument(
      entry.getFloor() <= entry.getCeiling(), "Ceiling value cannot be less than floor value.");
    Preconditions.checkArgument(
      entry.getStepSize() <= (entry.getCeiling() - entry.getFloor()), "Invalid step size.");

    int floor = (int) entry.getFloor();
    int ceiling = (int) entry.getCeiling();
    int step = (int) entry.getStepSize();

    if (floor == ceiling) {
      return floor;
    }
    int range = (ceiling - floor) / step;
    return floor + (step * rng.nextInt(range + 1));
  }

  /**
   * Class to compute the fitness score of an individual in the population.
   */
  private class ListFitnessEvaluator implements FitnessEvaluator<List<Integer>> {


    ListFitnessEvaluator(int populationSize) {
      // Nothing to do
    }

    @Override
    public double getFitness(List<Integer> candidate, List<? extends List<Integer>> population) {
      // Use the index of the candidate in the population to get the
      // corresponding
      // subject ID. Then get the fitness for that subject ID.
      int index = population.indexOf(candidate);

      // Look-up the last experiment.
      Experiment lastExperiment = null;

      lastExperiment = experimentDb.getLastExperiment();

      // Get the candidate's subject from the last experiment.
      long subjectId = lastExperiment.getSubjectIds().get(index);
      SubjectStateBridge subject = experimentDb.lookupSubject(subjectId);
      EvaluatedSubject evaluatedSubject = subject.getEvaluatedCopy();
      if (evaluatedSubject == null) {
        String cmdlineStr = subject.getCommandLine().toArgumentString();
        logger.log(
            Level.SEVERE, "subject returned marker for not evaluated. Subject: %s", cmdlineStr);
        throw new IllegalStateException(
            "subject returned marker for not evaluated. Subject: " + cmdlineStr);
      }

      // TODO(team): Consider whether/how to handle updates to weights or scorer between
      //    iterations. Should scores for choosing best performers be different from gene
      //    choosing? The main concern here is user expectation on when the (very
      //    infrequently used functionality) weight and scorer changes will be picked up.
      double fitness = evaluatedSubject.getFitness();

      // Add it to the population's total fitness. This is used for MONITORING PURPOSES.
      // NB: There's no way to increment this without overwriting the tmp variable.
      double tmpFitness = fitness;
      totalFitnessScore.addAndGet(tmpFitness);

      return fitness;
    }

    /**
     * Natural means best-fit individual has the highest fitness score.
     */
    @Override
    public boolean isNatural() {
      return true;
    }
  }

  /**
   * Mutates the genes of an individual.
   */
  private class IntegerListMutator implements EvolutionaryOperator<List<Integer>> {
    private final NumberGenerator<Probability> probability;

    IntegerListMutator(Probability mutationProbability) {
      probability = new ConstantGenerator<>(mutationProbability);
    }

    @Override
    public List<List<Integer>> apply(List<List<Integer>> selectedCandidates, Random rng) {
      SearchSpaceBundle bundle = config.getJvmSearchSpaceRestriction();
      List<List<Integer>> mutatedPopulation =
          Lists.newArrayListWithCapacity(selectedCandidates.size());
      for (List<Integer> selectedCandidate : selectedCandidates) {
        List<Integer> mutatedCandidate = Lists.newArrayListWithCapacity(selectedCandidate.size());
        for (int i = 0; i < selectedCandidate.size(); ++i) {
          if (probability.nextValue().nextEvent(rng)) {
            if (i == 0) {
              mutatedCandidate.add(rng.nextInt(supportedGcModes.size()));
            } else {
              SearchSpaceEntry entry = bundle.getSearchSpace(ARGUMENTS.get(i - 1));
              mutatedCandidate.add(generateRandomNumber(entry, rng));
            }
          } else {
            mutatedCandidate.add(selectedCandidate.get(i));
          }
        }
        mutatedPopulation.add(mutatedCandidate);
      }
      return mutatedPopulation;
    }
  }

  @Override
  public void startUp() {
    logger.info("Initializing Hypothesizer.");

    // TODO(team): This will need to be fixed such that metrics can be made pipeline.specific.

    metricExporter.register(
        "hypothesizer_population_size_total",
        "The population size of the experiment.",
        Metric.make(populationSize));
    metricExporter.register(
        "total_fitness_score",
        "The sum of the fitness score across the last experimental population.",
        Metric.make(totalFitnessScore));

    metricExporter.register(
        "hypthesizer_population_size_total",
        "DEPRECATED - USE hypothesizer_population_size_total - DEPRECATED",
        Metric.make(populationSize));
  }
}
