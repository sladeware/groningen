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

package org.arbeitspferde.groningen.validator;

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.PauseTime;
import org.arbeitspferde.groningen.experimentdb.ResourceMetric;
import org.arbeitspferde.groningen.experimentdb.SubjectRestart;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.subject.Subject;
import org.easymock.EasyMock;

import java.util.List;

/**
 * The test for {@link Validator}.
 */
public class ValidatorTest extends ClockedExperimentDbTestCaseBase {

  /** The object instance we are testing. */
  private Validator validator;

  private GroningenConfig mockGroningenConfig;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    experimentDb = EasyMock.createMock(ExperimentDb.class);
    
    mockGroningenConfig = EasyMock.createMock(GroningenConfig.class);

    validator = new Validator(clock, monitor, experimentDb, mockGroningenConfig, metricExporter);
  }

  /**
   * Check that profiledRun works without exception.
   *
   * TODO(team): Make less fragile.
   * TODO(team): Validate Protocol Buffer emissions.
   */
  public void testProfiledRun() throws Exception {
    final Experiment mockExperiment = EasyMock.createMock(Experiment.class);
    final SubjectStateBridge mockSubjectA = EasyMock.createMock(SubjectStateBridge.class);
    final SubjectStateBridge mockSubjectB = EasyMock.createMock(SubjectStateBridge.class);
    final List<SubjectStateBridge> subjects = Lists.newArrayList();
    final PauseTime mockSubjectAPauseTime = EasyMock.createMock(PauseTime.class);
    final ResourceMetric mockSubjectAResourceMetric = EasyMock.createMock(ResourceMetric.class);
    final PauseTime mockSubjectBPauseTime = EasyMock.createMock(PauseTime.class);
    final ResourceMetric mockSubjectBResourceMetric = EasyMock.createMock(ResourceMetric.class);
    final SubjectRestart mockSubjectASubjectRestart = EasyMock.createMock(SubjectRestart.class);
    final SubjectRestart mockSubjectBSubjectRestart = EasyMock.createMock(SubjectRestart.class);
    final CommandLine mockSubjectACommandLine = EasyMock.createMock(CommandLine.class);
    final CommandLine mockSubjectBCommandLine = EasyMock.createMock(CommandLine.class);
    final Subject mockSubjectAAssociatedSubject = EasyMock.createMock(Subject.class);
    final Subject mockSubjectBAssociatedSubject = EasyMock.createMock(Subject.class);
   
    final ProgramConfiguration programConfiguration =
        ProgramConfiguration.newBuilder().buildPartial();

    subjects.add(mockSubjectA);
    subjects.add(mockSubjectB);

    /* profiledRun */
    EasyMock.expect(experimentDb.getLastExperiment()).andReturn(mockExperiment);
    EasyMock.expect(mockExperiment.getSubjects()).andReturn(subjects);
    EasyMock.expect(mockSubjectA.getPauseTime()).andReturn(mockSubjectAPauseTime);
    EasyMock.expect(mockSubjectA.getResourceMetric()).andReturn(mockSubjectAResourceMetric);

    /* validate */
    EasyMock.expect(mockSubjectA.getAssociatedSubject()).andReturn(mockSubjectAAssociatedSubject)
        .atLeastOnce();
    EasyMock.expect(mockSubjectAAssociatedSubject.isDefault()).andReturn(false);
    EasyMock.expect(mockSubjectA.getSubjectRestart()).andReturn(mockSubjectASubjectRestart);
    EasyMock.expect(mockSubjectA.getIdOfObject()).andReturn(1L);
    EasyMock.expect(mockSubjectAAssociatedSubject.getServingAddress()).andReturn("/path/to/foo")
        .atLeastOnce();
    EasyMock.expect(mockSubjectASubjectRestart.restartThresholdCrossed(mockGroningenConfig))
        .andReturn(false);
    EasyMock.expect(mockSubjectASubjectRestart.didNotRun()).andReturn(false);
    EasyMock.expect(mockSubjectA.getCommandLine()).andReturn(mockSubjectACommandLine);
    EasyMock.expect(mockSubjectA.getCommandLineStrings()).andReturn(Lists.newArrayList("foo"));
    EasyMock.expect(mockSubjectACommandLine.toArgumentString()).andReturn("foo");
    EasyMock.expect(mockSubjectA.wasRemoved()).andReturn(false);

    /* profiledRun A again */
    mockSubjectA.markValid();
    EasyMock.expectLastCall();

    /* second time through loop */
    EasyMock.expect(mockSubjectB.getPauseTime()).andReturn(mockSubjectBPauseTime);
    EasyMock.expect(mockSubjectB.getResourceMetric()).andReturn(mockSubjectBResourceMetric);

    /* validate B */
    EasyMock.expect(mockSubjectB.getAssociatedSubject()).andReturn(mockSubjectBAssociatedSubject)
        .atLeastOnce();
    EasyMock.expect(mockSubjectBAssociatedSubject.isDefault()).andReturn(false);
    EasyMock.expect(mockSubjectB.getSubjectRestart()).andReturn(mockSubjectBSubjectRestart);
    EasyMock.expect(mockSubjectB.getIdOfObject()).andReturn(1L);
    EasyMock.expect(mockSubjectBAssociatedSubject.getServingAddress()).andReturn("/path/to/foo")
        .atLeastOnce();
    EasyMock.expect(mockSubjectBSubjectRestart.restartThresholdCrossed(mockGroningenConfig))
        .andReturn(false);
    EasyMock.expect(mockSubjectBSubjectRestart.didNotRun()).andReturn(false);
    EasyMock.expect(mockSubjectB.getCommandLine()).andReturn(mockSubjectBCommandLine);
    EasyMock.expect(mockSubjectB.getCommandLineStrings()).andReturn(Lists.newArrayList("bar"));
    EasyMock.expect(mockSubjectBCommandLine.toArgumentString()).andReturn("baz");
    EasyMock.expect(mockSubjectB.wasRemoved()).andReturn(false);

    /* profiledRun for B again */
    mockSubjectB.markInvalid();
    EasyMock.expectLastCall();
    mockSubjectBPauseTime.invalidate();
    EasyMock.expectLastCall();
    mockSubjectBResourceMetric.invalidate();
    EasyMock.expectLastCall();

    EasyMock.replay(experimentDb);
    EasyMock.replay(mockExperiment);
    EasyMock.replay(mockSubjectA);
    EasyMock.replay(mockSubjectB);
    EasyMock.replay(mockSubjectAAssociatedSubject);
    EasyMock.replay(mockSubjectBAssociatedSubject);
    EasyMock.replay(mockSubjectAPauseTime);
    EasyMock.replay(mockSubjectBPauseTime);
    EasyMock.replay(mockSubjectAResourceMetric);
    EasyMock.replay(mockSubjectBResourceMetric);
    EasyMock.replay(mockSubjectASubjectRestart);
    EasyMock.replay(mockSubjectBSubjectRestart);
    EasyMock.replay(mockSubjectACommandLine);
    EasyMock.replay(mockSubjectBCommandLine);
    EasyMock.replay(mockGroningenConfig);


    validator.profiledRun(mockGroningenConfig);

    EasyMock.verify(experimentDb);
    EasyMock.verify(mockExperiment);
    EasyMock.verify(mockSubjectA);
    EasyMock.verify(mockSubjectB);
    EasyMock.verify(mockSubjectAAssociatedSubject);
    EasyMock.verify(mockSubjectBAssociatedSubject);
    EasyMock.verify(mockSubjectAPauseTime);
    EasyMock.verify(mockSubjectBPauseTime);
    EasyMock.verify(mockSubjectAResourceMetric);
    EasyMock.verify(mockSubjectBResourceMetric);
    EasyMock.verify(mockSubjectASubjectRestart);
    EasyMock.verify(mockSubjectBSubjectRestart);
    EasyMock.verify(mockSubjectACommandLine);
    EasyMock.verify(mockSubjectBCommandLine);
    EasyMock.verify(mockGroningenConfig);
  }
}
