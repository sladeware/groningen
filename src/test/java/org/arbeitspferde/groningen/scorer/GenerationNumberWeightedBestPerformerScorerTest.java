package org.arbeitspferde.groningen.scorer;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.utility.PinnedClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tests for GenerationNumberWeightedBestPerformerScorer */
public class GenerationNumberWeightedBestPerformerScorerTest extends TestCase {

  /** Constants used for mocked clock source */
  protected static final long DEFAULT_TIME_MS = 1000000000L;
  protected static final long INCREMENT_MS = 1234L;

  /** Mocked clock source */
  private PinnedClock clock;

  /** ExperimentDb needed to make subjects */
  ExperimentDb experimentDb;

  /** subjects to use in tests */
  SubjectStateBridge subject1, subject2;

  /** Object under test */
  GenerationNumberWeightedBestPerformerScorer scorer;

  @Override
  protected void setUp() {
    clock = new PinnedClock(DEFAULT_TIME_MS, INCREMENT_MS);
    experimentDb = new ExperimentDb();

    scorer = new GenerationNumberWeightedBestPerformerScorer(clock);
  }

  /* Creates two individuals */
  private void createIndividuals() {
    final JvmFlagSet.Builder builder = JvmFlagSet.builder()
        .withValue(JvmFlag.HEAP_SIZE, 20)
        .withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 2)
        .withValue(JvmFlag.CMS_EXP_AVG_FACTOR, 3)
        .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, 4)
        .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, 5)
        .withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, 6)
        .withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, 7)
        .withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, 8)
        .withValue(JvmFlag.GC_TIME_RATIO, 9)
        .withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, 10)
        .withValue(JvmFlag.MAX_HEAP_FREE_RATIO, 11)
        .withValue(JvmFlag.MIN_HEAP_FREE_RATIO, 12)
        .withValue(JvmFlag.NEW_RATIO, 13)
        .withValue(JvmFlag.MAX_NEW_SIZE, 14)
        .withValue(JvmFlag.PARALLEL_GC_THREADS, 15)
        .withValue(JvmFlag.SURVIVOR_RATIO, 16)
        .withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, 17)
        .withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, 18)
        .withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, 19)
        .withValue(JvmFlag.CMS_INCREMENTAL_MODE, 1)
        .withValue(JvmFlag.CMS_INCREMENTAL_PACING, 0)
        .withValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY, 1)
        .withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, 1)
        .withValue(JvmFlag.USE_PARALLEL_GC, 0)
        .withValue(JvmFlag.USE_PARALLEL_OLD_GC, 0)
        .withValue(JvmFlag.USE_SERIAL_GC, 0);

    subject1 = experimentDb.makeSubject();
    subject1.storeCommandLine(builder.build());

    builder.withValue(JvmFlag.HEAP_SIZE, 40)
        .withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, 32)
        .withValue(JvmFlag.CMS_EXP_AVG_FACTOR, 33);

    subject2 = experimentDb.makeSubject();
    subject2.storeCommandLine(builder.build());
  }

  public void testDetectDuplicates() {
    createIndividuals();
    EvaluatedSubject evaledSubject1 = new EvaluatedSubject(clock, subject2, 21.0, 1);
    EvaluatedSubject evaledSubject2 = new EvaluatedSubject(clock, subject1, 24.0, 1);
    EvaluatedSubject evaledSubject3 = new EvaluatedSubject(clock, subject1, 22.0, 1);
    List<EvaluatedSubject> individuals = new ArrayList<>(3);
    individuals.add(evaledSubject1);
    individuals.add(evaledSubject2);
    individuals.add(evaledSubject3);
    Map<String, List<EvaluatedSubject>> cmdlineToIndividuals =
        scorer.detectDuplicates(individuals);
    assertEquals(2, cmdlineToIndividuals.size());
    assertNotNull(
        cmdlineToIndividuals.get(evaledSubject1.getBridge().getCommandLine().toArgumentString()));
    assertEquals(1, cmdlineToIndividuals.get(
        evaledSubject1.getBridge().getCommandLine().toArgumentString()).size());
    assertNotNull(
        cmdlineToIndividuals.get(evaledSubject2.getBridge().getCommandLine().toArgumentString()));
    assertEquals(2, cmdlineToIndividuals.get(
        evaledSubject2.getBridge().getCommandLine().toArgumentString()).size());
  }

  public void testCleanRecentRun() {
    createIndividuals();
    EvaluatedSubject evaledSubject1 = new EvaluatedSubject(clock, subject2, 21.0, 2);
    EvaluatedSubject evaledSubject2 = new EvaluatedSubject(clock, subject1, 24.0, 2);
    EvaluatedSubject evaledSubject3 = new EvaluatedSubject(clock, subject1, 22.0, 2);
    List<EvaluatedSubject> individuals = new ArrayList<>(3);
    individuals.add(evaledSubject1);
    individuals.add(evaledSubject2);
    individuals.add(evaledSubject3);
    Map<String, EvaluatedSubject> cmdlineToIndividuals =
        scorer.cleanRecentRun(individuals);
    assertEquals(2, cmdlineToIndividuals.size());
    assertNotNull(
        cmdlineToIndividuals.get(evaledSubject1.getBridge().getCommandLine().toArgumentString()));
    assertEquals(21.0, cmdlineToIndividuals.get(
        evaledSubject1.getBridge().getCommandLine().toArgumentString()).getFitness());
    assertNotNull(
        cmdlineToIndividuals.get(evaledSubject2.getBridge().getCommandLine().toArgumentString()));
    assertEquals(23.0, cmdlineToIndividuals.get(
        evaledSubject2.getBridge().getCommandLine().toArgumentString()).getFitness());
  }

  /* Basic test of correctness for a single generation */
  public void testAddGeneration_BasicCorrectNoMerges() {
    createIndividuals();
    EvaluatedSubject evaledSubject1 = new EvaluatedSubject(clock, subject2, 21.0, 2);
    EvaluatedSubject evaledSubject2 = new EvaluatedSubject(clock, subject1, 24.0, 2);
    List<EvaluatedSubject> generation1 = new ArrayList<>(2);
    generation1.add(evaledSubject1);
    generation1.add(evaledSubject2);

    // 1 merge in the first generation should have occurred
    List<EvaluatedSubject> gen1Processed = scorer.addGeneration(generation1);
    assertEquals(2, gen1Processed.size());

    List<EvaluatedSubject> alltimeBestList = scorer.getBestPerformers();
    assertEquals(2, alltimeBestList.size());
    assertEquals(24.0 * 2.0, alltimeBestList.get(0).getFitness());
  }

  /* Duplicates with different values, tests merging fitness */
  public void testAddGeneration_MergingFitness() {
    createIndividuals();
    EvaluatedSubject evaledSubjectPass1Num1 = new EvaluatedSubject(clock, subject2, 22.0, 1);
    EvaluatedSubject evaledSubjectPass1Num2 = new EvaluatedSubject(clock, subject1, 21.0, 1);
    EvaluatedSubject evaledSubjectPass1Num3 = new EvaluatedSubject(clock, subject2, 23.0, 1);
    List<EvaluatedSubject> generation = new ArrayList<>(3);
    generation.add(evaledSubjectPass1Num1);
    generation.add(evaledSubjectPass1Num2);
    generation.add(evaledSubjectPass1Num3);

    // 1 merge in the this generation should have occurred
    List<EvaluatedSubject> processedGeneration = scorer.addGeneration(generation);
    assertEquals(2, processedGeneration.size());
    assertEquals(22.5, processedGeneration.get(0).getFitness());
    assertEquals(21.0, processedGeneration.get(1).getFitness());
    List<EvaluatedSubject> alltimeBestList1 = scorer.getBestPerformers();
    assertEquals(2, alltimeBestList1.size());
    assertEquals(22.5, alltimeBestList1.get(0).getFitness());
    assertEquals(21.0, alltimeBestList1.get(1).getFitness());

    // next generation - no merges but all time scores will change
    generation.clear();
    EvaluatedSubject evaledSubjectPass2Num1 = new EvaluatedSubject(clock, subject1, 29.0, 2);
    EvaluatedSubject evaledSubjectPass2Num2 = new EvaluatedSubject(clock, subject2, 20.0, 2);
    generation.add(evaledSubjectPass2Num1);
    generation.add(evaledSubjectPass2Num2);
    processedGeneration = scorer.addGeneration(generation);
    assertEquals(2, processedGeneration.size());
    assertEquals(29.0, processedGeneration.get(0).getFitness());
    assertEquals(20.0, processedGeneration.get(1).getFitness());
    List<EvaluatedSubject> alltimeBestList2 = scorer.getBestPerformers();
    assertFalse(alltimeBestList2 == alltimeBestList1);
    assertEquals(2, alltimeBestList2.size());

    assertEquals(21.0 + 29.0 * 2, alltimeBestList2.get(0).getFitness());
    assertEquals(22.5 + 20 * 2, alltimeBestList2.get(1).getFitness());
  }

  public void testAddGeneration_EmptyGeneration() {
    List<EvaluatedSubject> generation = new ArrayList<>();
    List<EvaluatedSubject> processedGeneration = scorer.addGeneration(generation);
    assertEquals(0, processedGeneration.size());
  }
}
