/* Copyright 2013 Google, Inc.
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

package org.arbeitspferde.groningen.scorer;

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.eventlog.SubjectEventLogger;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.subject.Subject;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.easymock.EasyMock;

import java.util.List;

/**
 * The test for {@link IterationScorer}.
 */
public class IterationScorerTest extends ClockedExperimentDbTestCaseBase {

  /** The object instance we are testing. */
  private IterationScorer iterationScorer;
  private Experiment mockExperiment;
  private SubjectScorer mockSubjectScorer;
  private DisplayMediator mockDisplayMediator;
  private GroningenConfig mockGroningenConfig;
  private SubjectEventLogger mockSubjectEventLogger;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    experimentDb = EasyMock.createMock(ExperimentDb.class);
    
    mockExperiment = EasyMock.createMock(Experiment.class);
    
    mockGroningenConfig = EasyMock.createMock(GroningenConfig.class);

    mockSubjectScorer = EasyMock.createMock(SubjectScorer.class);

    mockDisplayMediator = EasyMock.createMock(DisplayMediator.class);
    
    mockSubjectEventLogger = EasyMock.createMock(SubjectEventLogger.class);

    iterationScorer = new IterationScorer(clock, mockDisplayMediator, experimentDb,
        mockSubjectScorer, mockSubjectEventLogger, metricExporter);
  }

  /**
   * Check that profiledRun works without exception.
   *
   * TODO(team): Make less fragile.
   * TODO(team): Validate Protocol Buffer emissions.
   */
  public void testProfiledRun() throws Exception {
    final SubjectStateBridge mockSubjectA = EasyMock.createMock(SubjectStateBridge.class);
    final SubjectStateBridge mockSubjectB = EasyMock.createMock(SubjectStateBridge.class);
    final List<SubjectStateBridge> subjects = Lists.newArrayList();
    final long experimentId = 3L;
    final double subjectAFitness = 10.0;
    final double subjectBFitness = 0.0;
    
    /* needed for evaluatedSubject constructor */
    final SubjectGroup mockSubjectGroup = EasyMock.createNiceMock(SubjectGroup.class);
    final Subject mockSubjectAAssociatedSubject = EasyMock.createMock(Subject.class);
    final Subject mockSubjectBAssociatedSubject = EasyMock.createMock(Subject.class);
    final int subjectAIndex = 1;
    final int subjectBIndex = 2;
    
    subjects.add(mockSubjectA);
    subjects.add(mockSubjectB);

    /* 
     * mockSubjectGroup is used within the call path, but isn't really under test and is
     * completely brittle. Set up to mock out the real stuff but don't bother checking
     */
    EasyMock.expect(mockSubjectGroup.getClusterName()).andReturn("xx").anyTimes();
    EasyMock.expect(mockSubjectGroup.getName()).andReturn("testjob").anyTimes();
    EasyMock.expect(mockSubjectGroup.getUserName()).andReturn("testuser").anyTimes();

    /* 
     * that we pass the same EvaluatedSubject to both be stored in the SubjectBridge and to the
     * monitor is one of the few things we would like to be able to test and its not testable.
     * configure mocks for the construction ahead of time...
     */
    EasyMock.expect(mockSubjectA.getAssociatedSubject()).andReturn(mockSubjectAAssociatedSubject)
        .anyTimes();
    EasyMock.expect(mockSubjectAAssociatedSubject.getGroup())
        .andReturn(mockSubjectGroup).anyTimes();
    EasyMock.expect(mockSubjectAAssociatedSubject.getIndex())
        .andReturn(subjectAIndex).anyTimes();
    EasyMock.expect(mockSubjectAAssociatedSubject.isDefault())
        .andReturn(false).anyTimes();
    EasyMock.expect(mockSubjectB.getAssociatedSubject()).andReturn(mockSubjectBAssociatedSubject)
        .anyTimes();
    EasyMock.expect(mockSubjectBAssociatedSubject.getGroup())
        .andReturn(mockSubjectGroup).anyTimes();
    EasyMock.expect(mockSubjectBAssociatedSubject.getIndex())
        .andReturn(subjectBIndex).anyTimes();
    EasyMock.expect(mockSubjectBAssociatedSubject.isDefault())
        .andReturn(false).anyTimes();

    /* profiledRun */
    EasyMock.expect(experimentDb.getLastExperiment()).andReturn(mockExperiment);
    EasyMock.expect(mockExperiment.getSubjects()).andReturn(subjects);
    EasyMock.expect(experimentDb.getExperimentId()).andReturn(experimentId);
    
    EasyMock.expect(mockSubjectScorer.compute(mockSubjectA, mockGroningenConfig))
        .andReturn(subjectAFitness);
    mockSubjectA.setEvaluatedCopy(EasyMock.anyObject(EvaluatedSubject.class));
    EasyMock.expectLastCall();
    mockDisplayMediator.addIndividual(EasyMock.anyObject(EvaluatedSubject.class));
    EasyMock.expectLastCall();
    mockSubjectEventLogger.logSubjectInExperiment(
        mockGroningenConfig, mockExperiment, mockSubjectA);
    EasyMock.expectLastCall();

    /* second time through loop */
    EasyMock.expect(mockSubjectScorer.compute(mockSubjectB, mockGroningenConfig))
        .andReturn(subjectBFitness);
    mockSubjectB.setEvaluatedCopy(EasyMock.anyObject(EvaluatedSubject.class));
    EasyMock.expectLastCall();
    mockDisplayMediator.addIndividual(EasyMock.anyObject(EvaluatedSubject.class));
    EasyMock.expectLastCall();
    mockSubjectEventLogger.logSubjectInExperiment(
        mockGroningenConfig, mockExperiment, mockSubjectB);
    EasyMock.expectLastCall();

    mockDisplayMediator.processGeneration();
    
    EasyMock.replay(experimentDb);
    EasyMock.replay(mockExperiment);
    EasyMock.replay(mockSubjectA);
    EasyMock.replay(mockSubjectB);
    EasyMock.replay(mockGroningenConfig);
    EasyMock.replay(mockSubjectAAssociatedSubject);
    EasyMock.replay(mockSubjectBAssociatedSubject);
    EasyMock.replay(mockSubjectGroup);
    EasyMock.replay(mockSubjectScorer);
    EasyMock.replay(mockSubjectEventLogger);

    iterationScorer.profiledRun(mockGroningenConfig);

    EasyMock.verify(experimentDb);
    EasyMock.verify(mockExperiment);
    EasyMock.verify(mockSubjectA);
    EasyMock.verify(mockSubjectB);
    EasyMock.verify(mockGroningenConfig);
    EasyMock.verify(mockSubjectAAssociatedSubject);
    EasyMock.verify(mockSubjectBAssociatedSubject);
    EasyMock.verify(mockSubjectScorer);
    EasyMock.verify(mockSubjectEventLogger);
  }
}
