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


import org.arbeitspferde.groningen.Pipeline;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.easymock.Capture;
import org.easymock.EasyMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The test for {@link DisplayMediator}
 */
public class DisplayMediatorTest extends ClockedExperimentDbTestCaseBase {
  private DisplayMediator mediator;
  private Pipeline pipelineMock;

  Object obj1, obj2;
  String displayString1, displayString2;
  SubjectStateBridge subject1, subject2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    pipelineMock = EasyMock.createNiceMock(Pipeline.class);
    EasyMock.expect(pipelineMock.id()).andReturn(new PipelineId("pipeline_id")).anyTimes();
    EasyMock.expect(
        pipelineManagerMock.findPipelineById(new PipelineId("pipeline_id")))
        .andReturn(pipelineMock).anyTimes();

    EasyMock.replay(pipelineMock, pipelineManagerMock, historyDataStoreMock);

    mediator = new DisplayMediator(experimentDb, historyDataStoreMock,
        pipelineManagerMock, new PipelineId("pipeline_id"), mockBestPerformerScorer);
    experimentDb.nextExperimentId();
  }

  /* Creates two objects to monitor */
  private void createObjects() {
    obj1 = new AtomicLong(21);
    obj2 = "I am a string";
    displayString1 = "test1";
    displayString2 = "test2";
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

  /* Tests the maxIndividuals method */
  public void testMaxIndividuals() {
    assertEquals(3, mediator.maxIndv);
    mediator.maxIndividuals(7);
    assertEquals(7, mediator.maxIndv);
  }

  /* Tests the addIndividual method */
  public void testAddIndividual() {
    createIndividuals();
    mediator.addIndividual(new EvaluatedSubject(clock, subject1, 21));
    mediator.addIndividual(new EvaluatedSubject(clock, subject2, 22));
    assertEquals(2, mediator.tempEvaluatedSubjects.size());
    assertEquals(21.0, mediator.tempEvaluatedSubjects.get(0).getFitness());
    assertEquals(22.0, mediator.tempEvaluatedSubjects.get(1).getFitness());
  }

  /* Tests the processGeneration method */
  public void testProcessGeneration() {
    createIndividuals();
    EvaluatedSubject evaledSubject1Pass1 = new EvaluatedSubject(clock, subject1, 21.0, 1);
    EvaluatedSubject evaledSubject2Pass1 = new EvaluatedSubject(clock, subject1, 21.0, 1);
    EvaluatedSubject evaledSubject1Pass2 = new EvaluatedSubject(clock, subject1, 21.0, 2);
    EvaluatedSubject evaledSubject2Pass2 = new EvaluatedSubject(clock, subject2, 24.0, 2);
    EvaluatedSubject evaledSubjectReturnPass1 = new EvaluatedSubject(clock, subject1, 21.0, 1);

    Capture<List<EvaluatedSubject>> captureGenList1 = new Capture<>();
    Capture<List<EvaluatedSubject>> captureGenList2 = new Capture<>();
    List<EvaluatedSubject> addGenReturnListPass1 = new ArrayList<>();
    addGenReturnListPass1.add(evaledSubjectReturnPass1);
    List<EvaluatedSubject> addGenReturnListPass2 = new ArrayList<>();
    addGenReturnListPass2.add(evaledSubject2Pass2);
    addGenReturnListPass2.add(evaledSubject1Pass2);
    EasyMock.expect(
        mockBestPerformerScorer.addGeneration(EasyMock.capture(captureGenList1)))
        .andReturn(addGenReturnListPass1);
    EasyMock.expect(
        mockBestPerformerScorer.addGeneration(EasyMock.capture(captureGenList2)))
        .andReturn(addGenReturnListPass2);
    EasyMock.replay(mockBestPerformerScorer);

    // first pass
    mediator.addIndividual(evaledSubject1Pass1);
    mediator.addIndividual(evaledSubject2Pass1);
    mediator.processGeneration();
    assertEquals(0, mediator.tempEvaluatedSubjects.size());
    assertEquals(1, mediator.currentEvaluatedSubjects.size());
    assertTrue(captureGenList1.hasCaptured());

    // second pass
    mediator.addIndividual(evaledSubject1Pass2);
    mediator.addIndividual(evaledSubject2Pass2);
    mediator.processGeneration();
    assertEquals(0, mediator.tempEvaluatedSubjects.size());
    assertEquals(2, mediator.currentEvaluatedSubjects.size());
  }

  /* Invoke processGeneration without adding individuals */
  public void testEmptyProcessGeneration() {
    Capture<List<EvaluatedSubject>> captureGenList = new Capture<>();
    List<EvaluatedSubject> emptySubjectList = new ArrayList<>();
    EasyMock.expect(
        mockBestPerformerScorer.addGeneration(EasyMock.capture(captureGenList)))
        .andReturn(emptySubjectList);
    EasyMock.replay(mockBestPerformerScorer);

    //do not add any individual
    mediator.processGeneration();
    assertEquals(0, mediator.tempEvaluatedSubjects.size());
    assertEquals(0, mediator.currentEvaluatedSubjects.size());
  }

  /* Tests the monitorObject method */
  public void testMonitorObject() {
    createObjects();
    mediator.monitorObject(obj1, displayString1);
    assertEquals(1, mediator.monitoredObjects.size());
    mediator.monitorObject(obj2, displayString2);
    assertEquals(2, mediator.monitoredObjects.size());
  }

  /* Tests the stopMonitoringObject method */
  public void testStopMonitoringObject() {
    // object not initialized
    assertFalse(mediator.stopMonitoringObject(obj2));
    // object initialized but not monitored
    createObjects();
    mediator.monitorObject(obj1, displayString1);
    assertFalse(mediator.stopMonitoringObject(obj2));
    // object initialized and monitored
    mediator.monitorObject(obj2, displayString1);
    assertTrue(mediator.stopMonitoringObject(obj1));
    assertEquals(1, mediator.monitoredObjects.size());
  }
}
