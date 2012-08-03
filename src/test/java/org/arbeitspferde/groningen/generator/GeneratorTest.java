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

package org.arbeitspferde.groningen.generator;


import org.arbeitspferde.groningen.common.ClockedExperimentDbTestCase;
import org.arbeitspferde.groningen.common.SubjectSettingsFileManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb.ExperimentCache;
import org.arbeitspferde.groningen.generator.Generator;
import org.arbeitspferde.groningen.generator.SubjectShuffler;
import org.easymock.EasyMock;

/**
 * The test for {@link Generator}.
 */
public class GeneratorTest extends ClockedExperimentDbTestCase {
  /** The object instance we are testing. */
  private Generator mockGenerator;
  private GroningenConfig mockConfig;
  private ExperimentCache mockExperimentCache;
  private Experiment mockExperiment;
  private SubjectShuffler mockShuffler;
  private SubjectSettingsFileManager mockSubjectSettingsFileManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    monitor = EasyMock.createMock(MonitorGroningen.class);
    experimentDb = EasyMock.createMock(ExperimentDb.class);
    mockConfig = EasyMock.createMock(GroningenConfig.class);
    mockExperimentCache = EasyMock.createMock(ExperimentCache.class);
    mockExperiment = EasyMock.createMock(Experiment.class);
    mockShuffler = EasyMock.createMock(SubjectShuffler.class);
    mockSubjectSettingsFileManager = EasyMock.createMock(SubjectSettingsFileManager.class);

    mockGenerator = new Generator(clock, monitor, experimentDb,
      "myservingaddress:31337", mockShuffler, mockSubjectSettingsFileManager,
      metricExporter);
  }

  /** Check that profiledRun works without exception. */
  public void testProfiledRun_WithExperiment() throws Exception {
    EasyMock.expect(experimentDb.getExperiments()).andReturn(mockExperimentCache);
    EasyMock.expect(mockExperimentCache.getLast()).andReturn(null);

    EasyMock.replay(monitor);
    EasyMock.replay(experimentDb);
    EasyMock.replay(mockConfig);
    EasyMock.replay(mockExperimentCache);
    EasyMock.replay(mockExperiment);
    EasyMock.replay(mockSubjectSettingsFileManager);

    mockGenerator.profiledRun(mockConfig);

    EasyMock.verify(monitor);
    EasyMock.verify(experimentDb);
    EasyMock.verify(mockConfig);
    EasyMock.verify(mockExperimentCache);
    EasyMock.verify(mockExperiment);
    EasyMock.verify(mockSubjectSettingsFileManager);
  }

  /** TODO(team): Add more tests as the Generator is implemented */
}
