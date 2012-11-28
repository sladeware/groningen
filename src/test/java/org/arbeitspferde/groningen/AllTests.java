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

package org.arbeitspferde.groningen;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.arbeitspferde.groningen.common.CmdProcessTest;
import org.arbeitspferde.groningen.common.EvaluatedSubjectTest;
import org.arbeitspferde.groningen.common.StatisticsTest;
import org.arbeitspferde.groningen.config.ProtoBufBinaryFileSourceTest;
import org.arbeitspferde.groningen.config.ProtoBufConfigManagerTest;
import org.arbeitspferde.groningen.config.ProtoBufConfigTest;
import org.arbeitspferde.groningen.config.ProtoBufSearchSpaceBundleTest;
import org.arbeitspferde.groningen.display.DisplayMediatorTest;
import org.arbeitspferde.groningen.display.GroningenServletTest;
import org.arbeitspferde.groningen.eventlog.EventLoggerServiceTest;
import org.arbeitspferde.groningen.eventlog.SafeProtoLoggerFactoryTest;
import org.arbeitspferde.groningen.eventlog.SafeProtoLoggerTest;
import org.arbeitspferde.groningen.executor.ExecutorTest;
import org.arbeitspferde.groningen.experimentdb.CommandLineTest;
import org.arbeitspferde.groningen.experimentdb.ComputeScoreTest;
import org.arbeitspferde.groningen.experimentdb.ExperimentDbTest;
import org.arbeitspferde.groningen.experimentdb.ExperimentTest;
import org.arbeitspferde.groningen.experimentdb.InMemoryCacheTest;
import org.arbeitspferde.groningen.experimentdb.PauseTimeTest;
import org.arbeitspferde.groningen.experimentdb.ResourceMetricTest;
import org.arbeitspferde.groningen.experimentdb.SubjectRestartTest;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridgeTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.DataSizeTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.FormattersTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.HotSpotFlagTypeTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSetTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagTest;
import org.arbeitspferde.groningen.experimentdb.jvmflags.ValueSeparatorTest;
import org.arbeitspferde.groningen.generator.GeneratorTest;
import org.arbeitspferde.groningen.hypothesizer.HypothesizerTest;
import org.arbeitspferde.groningen.profiling.ProfilingRunnableTest;
import org.arbeitspferde.groningen.validator.ValidatorTest;

public class AllTests extends TestSuite{

  public static Test suite() {
    TestSuite suite = new TestSuite();

    // org.arbeitspferde.groningen tests
    suite.addTestSuite(BaseModuleTest.class);
    suite.addTestSuite(PipelineIdGeneratorTest.class);
    suite.addTestSuite(PipelineIdTest.class);
    suite.addTestSuite(PipelineManagerTest.class);
    suite.addTestSuite(ServicesModuleTest.class);

    // org.arbeitspferde.groningen.common tests
    suite.addTestSuite(CmdProcessTest.class);
    suite.addTestSuite(EvaluatedSubjectTest.class);
    suite.addTestSuite(StatisticsTest.class);

    // org.arbeitspferde.groningen.config tests
    suite.addTestSuite(ProtoBufBinaryFileSourceTest.class);
    suite.addTestSuite(ProtoBufConfigManagerTest.class);
    suite.addTestSuite(ProtoBufConfigTest.class);
    suite.addTestSuite(ProtoBufSearchSpaceBundleTest.class);

    // org.arbeitspferde.groningen.display tests
    suite.addTestSuite(DisplayMediatorTest.class);
    suite.addTestSuite(GroningenServletTest.class);

    // org.arbeitspferde.groningen.eventlog tests
    suite.addTestSuite(EventLoggerServiceTest.class);
    suite.addTestSuite(SafeProtoLoggerFactoryTest.class);
    suite.addTestSuite(SafeProtoLoggerTest.class);

    // org.arbeitspferde.groningen.executor tests
    suite.addTestSuite(ExecutorTest.class);

    // org.arbeitspferde.groningen.experimentdb tests
    suite.addTestSuite(CommandLineTest.class);
    suite.addTestSuite(ComputeScoreTest.class);
    suite.addTestSuite(ExperimentDbTest.class);
    suite.addTestSuite(ExperimentTest.class);
    suite.addTestSuite(InMemoryCacheTest.class);
    suite.addTestSuite(PauseTimeTest.class);
    suite.addTestSuite(ResourceMetricTest.class);
    suite.addTestSuite(SubjectRestartTest.class);
    suite.addTestSuite(SubjectStateBridgeTest.class);

    // org.arbeitspferde.groningen.jvmflags tests
    suite.addTestSuite(DataSizeTest.class);
    suite.addTestSuite(FormattersTest.class);
    suite.addTestSuite(HotSpotFlagTypeTest.class);
    suite.addTestSuite(JvmFlagSetTest.class);
    suite.addTestSuite(JvmFlagTest.class);
    suite.addTestSuite(ValueSeparatorTest.class);

    // org.arbeitspferde.groningen.generator tests
    suite.addTestSuite(GeneratorTest.class);

    // org.arbeitspferde.groningen.hypothesizer tests
    suite.addTestSuite(HypothesizerTest.class);

    // org.arbeitspferde.groningen.profiling tests
    suite.addTestSuite(ProfilingRunnableTest.class);

    // org.arbeitspferde.groningen.validator tests
    suite.addTestSuite(ValidatorTest.class);

    return suite;
  }
}
