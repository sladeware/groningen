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

package org.arbeitspferde.groningen.eventlog;

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.PauseTime;
import org.arbeitspferde.groningen.experimentdb.ResourceMetric;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.proto.Event;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.arbeitspferde.groningen.subject.Subject;

import org.easymock.EasyMock;

/**
 * Tests for @{link SubjectEventProtoLogger}.
 */
public class SubjectEventProtoLoggerTest extends ClockedExperimentDbTestCaseBase {
  private static final long START_TIME = 1000L;

  /** The object instance we are testing. */
  private SubjectEventProtoLogger protoLogger;
  private EventLoggerService mockEventLoggerService;
  private SafeProtoLogger<Event.EventEntry> mockEventLogger;
  private String servingAddress;
  private Experiment mockExperiment;
  private GroningenConfig mockGroningenConfig;


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mockEventLoggerService = EasyMock.createMock(EventLoggerService.class);
    mockEventLogger = EasyMock.createMock(SafeProtoLogger.class);

    mockExperiment = EasyMock.createMock(Experiment.class);

    mockGroningenConfig = EasyMock.createMock(GroningenConfig.class);

    servingAddress = "myservingaddress:31337";

    protoLogger = new SubjectEventProtoLogger(clock, mockEventLoggerService,
        servingAddress, START_TIME, metricExporter);
  }

  /**
   * Check that logSubjectInExperiment works without exception.
   *
   * TODO(team): Make less fragile.
   * TODO(team): Validate Protocol Buffer emissions.
   */
  public void testLogSubjectInExperiment_withValidSubject() throws Exception {
    final SubjectStateBridge mockSubject = EasyMock.createMock(SubjectStateBridge.class);
    final PauseTime mockSubjectPauseTime = EasyMock.createMock(PauseTime.class);
    final ResourceMetric mockSubjectResourceMetric = EasyMock.createMock(ResourceMetric.class);

    final GroningenParamsOrBuilder mockGroningenParams =
        EasyMock.createMock(GroningenParamsOrBuilder.class);
    final ProgramConfiguration programConfiguration =
        ProgramConfiguration.newBuilder().buildPartial();
    final Subject mockSubjectAssociatedSubject = EasyMock.createMock(Subject.class);
    final long expId = 19042L;
    final int subjectAIndex = 14;
    final String subjectServingAddress = "some/network:address";
    final double latencyWeight = 2D;
    final double latencyScore = 4D;
    final double thruputWeight = 0D;
    final double thruputScore = 8D;
    final double memWeight = 5D;
    final double memScore = 10D;
    final Boolean subjectIsInvalid = Boolean.FALSE;
    final double subjectFitness = 10.0;

    /* logSubjectInExperiment */
    EasyMock.expect(mockEventLoggerService.getLogger()).andReturn(mockEventLogger);
    EasyMock.expect(mockExperiment.getIdOfObject()).andReturn(expId);

    /* composeEvent */
    EasyMock.expect(mockSubject.getPauseTime()).andReturn(mockSubjectPauseTime);
    EasyMock.expect(mockSubject.getResourceMetric()).andReturn(mockSubjectResourceMetric);

    EasyMock.expect(mockGroningenConfig.getParamBlock()).andReturn(mockGroningenParams);
    EasyMock.expect(mockGroningenParams.getLatencyWeight()).andReturn(latencyWeight).atLeastOnce();
    EasyMock.expect(mockGroningenParams.getThroughputWeight()).andReturn(thruputWeight)
        .atLeastOnce();
    EasyMock.expect(mockGroningenParams.getMemoryWeight()).andReturn(memWeight).atLeastOnce();
    EasyMock.expect(mockSubjectPauseTime.computeScore(PauseTime.ScoreType.LATENCY))
        .andReturn(latencyScore);
    EasyMock.expect(mockSubjectPauseTime.computeScore(PauseTime.ScoreType.THROUGHPUT))
        .andReturn(thruputScore);
    EasyMock.expect(mockSubjectResourceMetric.computeScore(ResourceMetric.ScoreType.MEMORY))
        .andReturn(memScore);
    EasyMock.expect(mockSubject.getAssociatedSubject()).andReturn(mockSubjectAssociatedSubject)
        .atLeastOnce();
    EasyMock.expect(mockSubjectAssociatedSubject.getServingAddress())
        .andReturn(subjectServingAddress).atLeastOnce();

    EasyMock.expect(mockSubject.isInvalid()).andReturn(subjectIsInvalid);
    EasyMock.expect(mockGroningenConfig.getProtoConfig()).andReturn(programConfiguration);
    EasyMock.expect(mockSubject.getPauseTime()).andReturn(mockSubjectPauseTime);
    EasyMock.expect(mockSubjectPauseTime.getPauseDurations()).andReturn(Lists.newArrayList(1D));

    /* back in logSubjectInExperiment */
    mockEventLogger.logProtoEntry(EasyMock.isA(Event.EventEntry.class));
    EasyMock.expectLastCall();

    EasyMock.replay(mockExperiment);
    EasyMock.replay(mockSubject);
    EasyMock.replay(mockSubjectPauseTime);
    EasyMock.replay(mockSubjectResourceMetric);
    EasyMock.replay(mockGroningenConfig);
    EasyMock.replay(mockGroningenParams);
    EasyMock.replay(mockSubjectAssociatedSubject);
    EasyMock.replay(mockEventLoggerService);
    EasyMock.replay(mockEventLogger);

    protoLogger.logSubjectInExperiment(mockGroningenConfig, mockExperiment, mockSubject);

    EasyMock.verify(mockEventLoggerService);
    EasyMock.verify(mockExperiment);
    EasyMock.verify(mockSubject);
    EasyMock.verify(mockSubjectPauseTime);
    EasyMock.verify(mockSubjectResourceMetric);
    EasyMock.verify(mockGroningenConfig);
    EasyMock.verify(mockGroningenParams);
    EasyMock.verify(mockSubjectAssociatedSubject);
    EasyMock.verify(mockEventLoggerService);
    EasyMock.verify(mockEventLogger);
  }

  public void testLogSubjectInExperiment_withUnvalidateSubject() throws Exception {
    final SubjectStateBridge mockSubject = EasyMock.createMock(SubjectStateBridge.class);
    final PauseTime mockSubjectPauseTime = EasyMock.createMock(PauseTime.class);
    final ResourceMetric mockSubjectResourceMetric = EasyMock.createMock(ResourceMetric.class);

    final GroningenParamsOrBuilder mockGroningenParams =
        EasyMock.createMock(GroningenParamsOrBuilder.class);
    final ProgramConfiguration programConfiguration =
        ProgramConfiguration.newBuilder().buildPartial();
    final Subject mockSubjectAssociatedSubject = EasyMock.createMock(Subject.class);
    final long expId = 142L;
    final int subjectAIndex = 1;
    final String subjectServingAddress = "some/network:address";
    final double latencyWeight = 2D;
    final double latencyScore = 8D;
    final double thruputWeight = 0D;
    final double thruputScore = 2D;
    final double memWeight = 5D;
    final double memScore = 20D;
    final Boolean subjectIsInvalid = null;
    final double subjectFitness = 0D;

    /* logSubjectInExperiment */
    EasyMock.expect(mockEventLoggerService.getLogger()).andReturn(mockEventLogger);
    EasyMock.expect(mockExperiment.getIdOfObject()).andReturn(expId);

    /* composeEvent */
    EasyMock.expect(mockSubject.getPauseTime()).andReturn(mockSubjectPauseTime);
    EasyMock.expect(mockSubject.getResourceMetric()).andReturn(mockSubjectResourceMetric);

    EasyMock.expect(mockGroningenConfig.getParamBlock()).andReturn(mockGroningenParams);
    EasyMock.expect(mockGroningenParams.getLatencyWeight()).andReturn(latencyWeight).atLeastOnce();
    EasyMock.expect(mockGroningenParams.getThroughputWeight()).andReturn(thruputWeight)
        .atLeastOnce();
    EasyMock.expect(mockGroningenParams.getMemoryWeight()).andReturn(memWeight).atLeastOnce();
    EasyMock.expect(mockSubjectPauseTime.computeScore(PauseTime.ScoreType.LATENCY))
        .andReturn(latencyScore);
    EasyMock.expect(mockSubjectPauseTime.computeScore(PauseTime.ScoreType.THROUGHPUT))
        .andReturn(thruputScore);
    EasyMock.expect(mockSubjectResourceMetric.computeScore(ResourceMetric.ScoreType.MEMORY))
        .andReturn(memScore);
    EasyMock.expect(mockSubject.getAssociatedSubject()).andReturn(mockSubjectAssociatedSubject)
        .atLeastOnce();
    EasyMock.expect(mockSubjectAssociatedSubject.getServingAddress())
        .andReturn(subjectServingAddress).atLeastOnce();

    EasyMock.expect(mockSubject.isInvalid()).andReturn(subjectIsInvalid);
    EasyMock.expect(mockGroningenConfig.getProtoConfig()).andReturn(programConfiguration);
    EasyMock.expect(mockSubject.getPauseTime()).andReturn(mockSubjectPauseTime);
    EasyMock.expect(mockSubjectPauseTime.getPauseDurations()).andReturn(Lists.newArrayList(1D));

    /* back in logSubjectInExperiment */
    mockEventLogger.logProtoEntry(EasyMock.isA(Event.EventEntry.class));
    EasyMock.expectLastCall();

    EasyMock.replay(mockExperiment);
    EasyMock.replay(mockSubject);
    EasyMock.replay(mockSubjectPauseTime);
    EasyMock.replay(mockSubjectResourceMetric);
    EasyMock.replay(mockGroningenConfig);
    EasyMock.replay(mockGroningenParams);
    EasyMock.replay(mockSubjectAssociatedSubject);
    EasyMock.replay(mockEventLoggerService);
    EasyMock.replay(mockEventLogger);

    protoLogger.logSubjectInExperiment(mockGroningenConfig, mockExperiment, mockSubject);

    EasyMock.verify(mockEventLoggerService);
    EasyMock.verify(mockExperiment);
    EasyMock.verify(mockSubject);
    EasyMock.verify(mockSubjectPauseTime);
    EasyMock.verify(mockSubjectResourceMetric);
    EasyMock.verify(mockGroningenConfig);
    EasyMock.verify(mockGroningenParams);
    EasyMock.verify(mockSubjectAssociatedSubject);
    EasyMock.verify(mockEventLoggerService);
    EasyMock.verify(mockEventLogger);
  }
}
