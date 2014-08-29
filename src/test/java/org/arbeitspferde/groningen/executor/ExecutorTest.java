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

package org.arbeitspferde.groningen.executor;


import org.arbeitspferde.groningen.PipelineStageInfo;
import org.arbeitspferde.groningen.PipelineSynchronizer;
import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCaseBase;
import org.arbeitspferde.groningen.common.SubjectSettingsFileManager;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.externalprocess.ProcessInvoker;
import org.arbeitspferde.groningen.extractor.CollectionLogAddressor;
import org.arbeitspferde.groningen.subject.HealthQuerier;
import org.arbeitspferde.groningen.subject.SubjectInterrogator;
import org.arbeitspferde.groningen.subject.SubjectManipulator;
import org.arbeitspferde.groningen.subject.open.NullServingAddressGenerator;
import org.arbeitspferde.groningen.utility.FileFactory;
import org.arbeitspferde.groningen.utility.MetricExporter;

import org.easymock.EasyMock;

/**
 * The test for {@link Executor}.
 */
public class ExecutorTest extends ClockedExperimentDbTestCaseBase {
  /** The object instance we are testing. */
  private Executor executor;
  private ProcessInvoker mockInvoker;
  private HealthQuerier mockHealthQuerier;
  private SubjectInterrogator mockSubjectInterrogator;
  private PipelineSynchronizer mockPipelineSynchronizer;
  private SubjectSettingsFileManager mockSubjectSettingsFileManager;
  private MetricExporter mockMetricExporter;
  private FileFactory mockFileFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mockInvoker = EasyMock.createMock(ProcessInvoker.class);
    EasyMock.replay(mockInvoker);

    mockHealthQuerier = EasyMock.createMock(HealthQuerier.class);
    EasyMock.replay(mockHealthQuerier);

    mockSubjectInterrogator = EasyMock.createMock(SubjectInterrogator.class);
    EasyMock.replay(mockSubjectInterrogator);

    mockPipelineSynchronizer = EasyMock.createMock(PipelineSynchronizer.class);
    EasyMock.replay(mockPipelineSynchronizer);

    mockSubjectSettingsFileManager = EasyMock.createMock(SubjectSettingsFileManager.class);
    EasyMock.replay(mockSubjectSettingsFileManager);

    mockMetricExporter = EasyMock.createMock(MetricExporter.class);
    EasyMock.replay(mockMetricExporter);

    mockFileFactory = EasyMock.createMock(FileFactory.class);
    EasyMock.replay(mockFileFactory);

    final SubjectManipulator mockManipulator = EasyMock.createNiceMock(SubjectManipulator.class);
    EasyMock.replay(mockManipulator);

    final CollectionLogAddressor mockCollectionLogAddressor =
        EasyMock.createNiceMock(CollectionLogAddressor.class);
    EasyMock.replay(mockCollectionLogAddressor);

    final PipelineStageInfo pipelineStageInfo = new PipelineStageInfo();

    executor = new Executor(clock, monitor, experimentDb, mockManipulator, mockHealthQuerier,
        mockSubjectInterrogator, mockPipelineSynchronizer, mockSubjectSettingsFileManager,
        mockMetricExporter, mockFileFactory, new NullServingAddressGenerator(),
        mockCollectionLogAddressor, pipelineStageInfo);
  }

  /** Check that profiledRun works without exception. */
  public void testProfiledRun() throws Exception {
    ConfigManager cm = new StubConfigManager();
    GroningenConfig config = cm.queryConfig();
    executor.profiledRun(config);
  }

  /** TODO(team): Implement more tests as the Executor is implemented */
}
