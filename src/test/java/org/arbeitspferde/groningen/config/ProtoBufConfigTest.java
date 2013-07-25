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

package org.arbeitspferde.groningen.config;


import junit.framework.TestCase;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.JvmSearchSpace;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.easymock.EasyMock;

/**
 * Tests for the {@link ProtoBufConfig} implementation of {@link GroningenConfig} including the
 * the {@link ProtoBufConfig.ProtoBufClusterConfig ProtoBufClusterConfig},
 * {@link ProtoBufConfig.ProtoSubjectGroupConfig ProtoBufGroupConfig}, and
 * {@link ProtoBufConfig.ProtoBufSubjectConfig ProtoBufSubjectConfig} inner classes.
 */
public class ProtoBufConfigTest extends TestCase {
  static final String CLUSTER_NAME = "yy";
  static final String SUBJECT_GROUP_NAME = "testGroup1";
  static final String INPUT_LOG_NAME = "stdin";
  static final String USER = "theAllMightyUser";
  static final String EXP_SETTINGS_FILES_DIR_1 = "/some/path1";
  static final String EXP_SETTINGS_FILES_DIR_2 = "/some/path2";
  static final String DHT_USER = "btuser";
  static final String DHT_CLUSTER = "zz";
  static final int SUBJECT_COUNT = 0;
  static final int SUBJECT_WARMUP_TIMEOUT = 60;
  static final int SUBJECT_COUNT_WITH_DEFAULT_SETTINGS = 0;

  ProtoBufConfig.ProtoBufConfigConstructorFactory mockFactory;

  @Override
  public void setUp() {
    mockFactory = EasyMock.createMock(ProtoBufConfig.ProtoBufConfigConstructorFactory.class);
    ProtoBufConfig.setConstructorFactory(mockFactory);
  }

  /*
   * End to End test. Well, at least all classes nested within ProtoBufConfig, using the classes
   * created by the associated protobuf we are wrapping
   */
  public void testConstructorWithTinyValidProto() throws Exception {
    ProtoBufConfig.setConstructorFactory(
        new ProtoBufConfig.ProtoBufConfigConstructorFactory());
    ProgramConfiguration protoConfig = createTinyValidConfig();
    ProtoBufConfig jtuneConfig = null;
    jtuneConfig = new ProtoBufConfig(protoConfig);
    verifyTinyValidConfig(jtuneConfig);
  }

  /*
   * Top level ProtoBufConf tests
   */
  public void testProtoBufConfigWithMissingParamBlock() throws Exception {
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .addCluster(createValidClusterConfig())
        .build();

    try {
      ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // there is a missing param_block definition which requries at least the gc log name to be
      // defined
    }
  }

  public void testProtoBufConfigWithInvalidParamBlock() throws Exception {
    // this test is only valid as long as there are required fields with no defaults in
    // the param block. Remove (or comment out) if that is not longer true
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .addCluster(createValidClusterConfig())
        .setParamBlock(GroningenParams.newBuilder().build())
        .build();

    try {
      ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // there is a missing param_block definition which requries at least the gc log name to be
      // defined
    }
  }

  public void testProtoBufConfigWithNoClusters() throws Exception {
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .setParamBlock(createValidParamBlock())
        .build();

    try {
      ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // valid configurations must contain at least 1 cluster definition
    }
  }

  public void testProtoBufConfigWithInvalidCluster() throws Exception {
    // missing cluster name
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .setParamBlock(createValidParamBlock())
        .addCluster(ProgramConfiguration.ClusterConfig.newBuilder()
            .addSubjectGroup(createValidSubjectGroupConfig())
            .build())
        .build();

    try {
      ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // InvalidConfiguraitonExceptions from the cluster level should be propagated up
    }
  }

  public void testProtoBufConfigWithMultiClusters() throws Exception {
    String cluster1Name = "aa";
    String cluster2Name = "bb";
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .setParamBlock(createValidParamBlock())
        .addCluster(ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(cluster1Name)
            .addSubjectGroup(createValidSubjectGroupConfig())
            .build())
        .addCluster(ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(cluster2Name)
            .addSubjectGroup(createValidSubjectGroupConfig())
            .build())
        .build();

    ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
    assertEquals("cluster list size", 2, testConfig.getClusterConfigs().size());
    assertTrue(
        "cluster1 omitted", testConfig.getClusterConfigs().get(0).getName().equals(cluster1Name)
        || testConfig.getClusterConfigs().get(1).getName().equals(cluster1Name));
    assertTrue(
        "cluster2 omitted", testConfig.getClusterConfigs().get(0).getName().equals(cluster2Name)
        || testConfig.getClusterConfigs().get(1).getName().equals(cluster2Name));
  }

  public void testProtoBufConfigGetClusterConfigByName() throws Exception {
    String cluster1Name = "aa";
    String cluster2Name = "bb";
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder()
        .setParamBlock(createValidParamBlock())
        .addCluster(ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(cluster1Name)
            .addSubjectGroup(createValidSubjectGroupConfig())
            .build())
        .addCluster(ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(cluster2Name)
            .addSubjectGroup(createValidSubjectGroupConfig())
            .build())
        .build();

    ProtoBufConfig testConfig = new ProtoBufConfig(protobufConfig);
    assertEquals("cluster1", cluster1Name, testConfig.getClusterConfig(cluster1Name).getName());
    assertEquals("cluster2", cluster2Name, testConfig.getClusterConfig(cluster2Name).getName());
  }

  /*
   * ProtoBufClusterConfig tests
   */
  public void testClusterConfigConstructionWithValidParams() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration.ClusterConfig protobufClusterConfig = createValidClusterConfig();
    EasyMock.replay(mockTopLevel);

    ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
        new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
    assertEquals("cluster name", CLUSTER_NAME, testClusterConfig.getName());
    assertEquals("group list size", 1, testClusterConfig.getSubjectGroupConfigs().size());
    assertEquals("group", SUBJECT_GROUP_NAME,
        testClusterConfig.getSubjectGroupConfigs().get(0).getName());
  }

  public void testClusterConfigConstructionWithMultiGroups() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    String group1Name = "group1";
    String group2Name = "group2";
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(CLUSTER_NAME)
            .setUser(USER)
            .setNumberOfSubjects(SUBJECT_COUNT)
            .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
            .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
            .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
                .setSubjectGroupName(group1Name)
                .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
                .build())
            .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
                .setSubjectGroupName(group2Name)
                .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_2)
                .build())
            .build();
    EasyMock.replay(mockTopLevel);

    ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
        new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
    assertEquals("group list size", 2, testClusterConfig.getSubjectGroupConfigs().size());
    assertTrue(
        "group 1 present",
        testClusterConfig.getSubjectGroupConfigs().get(0).getName().equals(group1Name)
        || testClusterConfig.getSubjectGroupConfigs().get(1).getName().equals(group1Name));
    assertTrue(
        "group 2 present",
        testClusterConfig.getSubjectGroupConfigs().get(0).getName().equals(group2Name)
        || testClusterConfig.getSubjectGroupConfigs().get(1).getName().equals(group2Name));
  }

  public void testClusterConfigGetGroupByNameWithMultiGroups() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    String group1Name = "group1";
    String group2Name = "group2";
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
            .setCluster(CLUSTER_NAME)
            .setUser(USER)
            .setNumberOfSubjects(SUBJECT_COUNT)
            .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
            .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
            .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
                .setSubjectGroupName(group1Name)
                .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
                .build())
            .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
                .setSubjectGroupName(group2Name)
                .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_2)
                .build())
            .build();
    EasyMock.replay(mockTopLevel);

    ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
        new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
    assertEquals("group 1", group1Name,
        testClusterConfig.getSubjectGroupConfig(group1Name).getName());
    assertEquals("group 2", group2Name,
        testClusterConfig.getSubjectGroupConfig(group2Name).getName());
  }

  public void testClusterConfigConstructionThrowsWithNoGroups() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
        .setCluster(CLUSTER_NAME)
        .build();
    EasyMock.replay(mockTopLevel);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // must have at least one group to experiment on in the cluster
    }
  }

  public void testClusterConfigThrowsWithDuplicateGroupNames() throws Exception {
    String group1Name = "group1";
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
        .setCluster("cc")
        .setUser(USER)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
            .setSubjectGroupName(group1Name)
            .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
            .build())
        .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
            .setSubjectGroupName(group1Name)
            .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_2)
            .build())
        .build();
    EasyMock.replay(mockTopLevel);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // Groups cannot have duplicate names in the same cluster
    }
  }

  public void testClusterConfigConstructionThrowsWithNullProto() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    EasyMock.replay(mockTopLevel);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(null, mockTopLevel);
      fail("did not generate exception");
    } catch (NullPointerException expected) {
      // the proto to wrap must be included or we have nothing to wrap which is invalid
    }
  }

  public void testClusterConfigConstructionThrowsWithNullParent() throws Exception {
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
        .setCluster("cc")
        .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
            .setSubjectGroupName(SUBJECT_GROUP_NAME)
            .setUser(USER)
            .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
            .setNumberOfSubjects(SUBJECT_COUNT)
            .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
            .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
            .build())
        .build();
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, null);
      fail("did not generate exception");
    } catch (NullPointerException expected) {
      // at the cluster level and below, backpointers must be included to search for values
      // omitted at lower levels of the tree structure
    }
  }

  public void testClusterConfigConstructionThrowsWithNoCluster() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
        .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
            .setSubjectGroupName(SUBJECT_GROUP_NAME)
            .setUser(USER)
            .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
            .setNumberOfSubjects(SUBJECT_COUNT)
            .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
            .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
            .build())
        .build();
    EasyMock.replay(mockTopLevel);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // without a name, we do not know where the groups are actually located
    }
  }

  public void testClusterConfigConstructionThrowsWithInvalidGroup() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProgramConfiguration.ClusterConfig protobufClusterConfig =
        ProgramConfiguration.ClusterConfig.newBuilder()
        .setCluster(CLUSTER_NAME)
        .addSubjectGroup(ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
            .setUser(USER)
            .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
            .setNumberOfSubjects(SUBJECT_COUNT)
            .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
            .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
            .build())
        .build();
    EasyMock.replay(mockTopLevel);

    try {
      ProtoBufConfig.ProtoBufClusterConfig testClusterConfig =
          new ProtoBufConfig.ProtoBufClusterConfig(protobufClusterConfig, mockTopLevel);
      fail("did not throw expected exception");
    } catch (InvalidConfigurationException expected) {
      // group level exceptions should be propagated up
    }
  }

  /*
   * ProtoBufGroupConfig tests
   */
  public void testGroupConfigConstructionWithValidParamsAndNoSubjects() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();
    EasyMock.replay(mockCluster);

    verifyValidGroupConfig(protobufGroupConfig, mockCluster);
  }

  public void testGroupConfigConstructionWithUserInCluster() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();

    EasyMock.expect(mockCluster.retrieveFirstUser()).andReturn(USER).atLeastOnce();
    EasyMock.replay(mockCluster);

    verifyValidGroupConfig(protobufGroupConfig, mockCluster);
  }

  public void testGroupConfigConstructionWithUserInBothGroupAndCluster() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();
    EasyMock.expect(mockCluster.retrieveFirstUser()).andReturn(USER).anyTimes();
    EasyMock.replay(mockCluster);

    verifyValidGroupConfig(protobufGroupConfig, mockCluster);
  }

  private void verifyValidGroupConfig(
      final ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig,
      final ProtoBufConfig.ProtoBufClusterConfig cluster) throws Exception {
    ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
        new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, cluster);
    assertEquals("group name", SUBJECT_GROUP_NAME, testGroupConfig.getName());
    assertEquals("user", USER, testGroupConfig.getUser());
    assertEquals("partial path", EXP_SETTINGS_FILES_DIR_1,
        testGroupConfig.getExperimentSettingsFilesDir());
    assertEquals("empty subject list", 0, testGroupConfig.getSpecialSubjectConfigs().size());
  }

  public void testGroupConfigConstructionWithValidSubjects() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .addSubjectConfig(
            ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig
            .newBuilder()
            .setSubjectIndex(1)
            .build())
        .addSubjectConfig(
            ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig
            .newBuilder()
            .setSubjectIndex(2)
            .build())
        .build();
    EasyMock.replay(mockCluster);

    ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
        new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
    assertEquals("subject list length", 2, testGroupConfig.getSpecialSubjectConfigs().size());
    // verify both got constructed by adding their ids
    int idSum = 0;
    for (ProtoBufConfig.SubjectConfig subject : testGroupConfig.getSpecialSubjectConfigs()) {
      idSum += subject.getSubjectIndex();
    }
    assertEquals("subject id sum", 3, idSum);
  }

  // negative tests
  public void testGroupConfigConstructionThrowsWithNullProto() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(null, mockCluster);
      fail("did not generate exception");
    } catch (NullPointerException expected) {
      // must have a proto to wrap
    }
  }

  public void testGroupConfigConstructionThrowsWithNullParent() throws Exception {
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        createValidSubjectGroupConfig();
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, null);
      fail("did not generate exception");
    } catch (NullPointerException expected) {
      // at the cluster level and below, backpointers must be included to search for values
      // omitted at lower levels of the tree structure
    }
  }

  public void testGroupConfigConstructionThrowsWithNoUser() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder().build();
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();
    EasyMock.expect(mockCluster.retrieveFirstUser()).andReturn(null).atLeastOnce();
    EasyMock.replay(mockTopLevel);
    EasyMock.replay(mockCluster);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // without a username/mdb group under to run the subject, we cannot guarantee the task will
      // run
    }
  }

  public void testGroupConfigConstructionThrowsWithNegativeNumberSubjectsUpstream()
      throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder().build();
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .build();
    EasyMock.expect(mockCluster.retrieveFirstNumberOfSubjects()).andReturn(-1).atLeastOnce();
    EasyMock.replay(mockTopLevel);
    EasyMock.replay(mockCluster);
    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // number of subjects needs to be nonegative
    }
  }

  public void testGroupConfigConstructionThrowsWithNoSubjectWarmup() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration protobufConfig = ProgramConfiguration.newBuilder().build();
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();
    EasyMock.expect(mockCluster.retrieveFirstSubjectWarmupTimeout()).andReturn(0).atLeastOnce();
    EasyMock.replay(mockTopLevel);
    EasyMock.replay(mockCluster);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // the default time to wait for subjects to warmup before we conclude a task is unhealthy
      // must be positive (even if it the default value)
    }
  }

  public void testGroupConfigConstructionThrowsWithNoExpSettingsFilesDir() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .build();
    EasyMock.replay(mockTopLevel);
    EasyMock.replay(mockCluster);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // without a path to the experimental settings file, experimental settings cannot be conveyed
      // to the subjects
    }
  }

  public void testGroupConfigConstructionThrowsWithInvalidExpSettingsFilesDir() throws Exception {
    ProtoBufConfig mockTopLevel = EasyMock.createMock(ProtoBufConfig.class);
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir("relative/path")
        .build();
    EasyMock.replay(mockTopLevel);
    EasyMock.replay(mockCluster);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // exp_settings_files_dir should be absolute, not relative
    }
  }

  public void testGroupConfigConstructionThrowsWithInvalidSubject() throws Exception {
    ProtoBufConfig.ProtoBufClusterConfig mockCluster =
        EasyMock.createMock(ProtoBufConfig.ProtoBufClusterConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig protobufGroupConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .addSubjectConfig(
            ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig
            .newBuilder()
            .setSubjectIndex(-1)
            .build())
        .build();
    EasyMock.replay(mockCluster);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoSubjectGroupConfig testGroupConfig =
          new ProtoBufConfig.ProtoSubjectGroupConfig(protobufGroupConfig, mockCluster);
      fail("did not generate exception");
    } catch (InvalidConfigurationException expected) {
      // subject level exceptions should propagate up as InvalidConfiguraitonExceptions
    }
  }

  /*
   * ProtoBufSubjectConfig tests.
   */
  public void testSubjectConfigConstructionWithValidIdAndNoJvmParams() throws Exception {
    int subjectIndex = 101;
    ProtoBufConfig.ProtoSubjectGroupConfig mockGroup =
        EasyMock.createMock(ProtoBufConfig.ProtoSubjectGroupConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig protobufSubjectConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig.newBuilder()
        .setSubjectIndex(subjectIndex)
        .build();
    EasyMock.replay(mockGroup);
    EasyMock.replay(mockFactory);

    ProtoBufConfig.ProtoBufSubjectConfig testSubjectConfig =
        new ProtoBufConfig.ProtoBufSubjectConfig(protobufSubjectConfig, mockGroup);
    assertNotNull(testSubjectConfig);
    assertNull(testSubjectConfig.getJvmSearchSpaceDefinition());
    assertEquals("subject id", new Integer(subjectIndex), testSubjectConfig.getSubjectIndex());
    assertEquals("parent", mockGroup, testSubjectConfig.getParentSubjectGroup());
  }

  public void testSubjectConfigConstructionWithInvalidIdAndNoJvmParams() throws Exception {
    int subjectIndex = -10;
    ProtoBufConfig.ProtoSubjectGroupConfig mockGroup =
        EasyMock.createMock(ProtoBufConfig.ProtoSubjectGroupConfig.class);
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig protobufSubjectConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig.newBuilder()
        .setSubjectIndex(subjectIndex)
        .build();
    EasyMock.replay(mockGroup);
    EasyMock.replay(mockFactory);

    try {
      ProtoBufConfig.ProtoBufSubjectConfig testSubjectConfig =
          new ProtoBufConfig.ProtoBufSubjectConfig(protobufSubjectConfig, mockGroup);
      fail("no exception thrown");
    } catch (InvalidConfigurationException expected) {
      // subject id must be nonnegative
    }
  }

  public void testSubjectConfigConstructionWithValidIdAndJvmParams() throws Exception {
    int subjectIndex = 10;
    ProtoBufConfig.ProtoSubjectGroupConfig mockGroup =
        EasyMock.createMock(ProtoBufConfig.ProtoSubjectGroupConfig.class);
    JvmSearchSpace protobufSearchSpace = JvmSearchSpace.newBuilder().build();
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig protobufSubjectConfig =
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig.newBuilder()
        .setSubjectIndex(subjectIndex)
        .setJvmParameters(protobufSearchSpace)
        .build();
    ProtoBufSearchSpaceBundle mockSearchSpace =
        EasyMock.createMock(ProtoBufSearchSpaceBundle.class);

    EasyMock.expect(mockFactory.createProtoBufSearchSpaceBundle(protobufSearchSpace, true))
        .andReturn(mockSearchSpace);

    EasyMock.replay(mockFactory);
    EasyMock.replay(mockGroup);
    EasyMock.replay(mockSearchSpace);

    ProtoBufConfig.ProtoBufSubjectConfig testSubjectConfig =
        new ProtoBufConfig.ProtoBufSubjectConfig(protobufSubjectConfig, mockGroup);

    assertNotNull(testSubjectConfig);
    assertEquals("search space", mockSearchSpace, testSubjectConfig.getJvmSearchSpaceDefinition());
    assertEquals("subject id", new Integer(subjectIndex), testSubjectConfig.getSubjectIndex());
    assertEquals("parent", mockGroup, testSubjectConfig.getParentSubjectGroup());
  }

  /*
   * helper functions for creating and validating larger protobuffer class based configs
   */
  private ProgramConfiguration createTinyValidConfig() {
    return ProgramConfiguration.newBuilder()
        .addCluster(createValidClusterConfig())
        .setParamBlock(createValidParamBlock())
        .build();
  }

  private GroningenParams createValidParamBlock() {
    return GroningenParams.newBuilder().setInputLogName(INPUT_LOG_NAME).build();
  }

  private ProgramConfiguration.ClusterConfig createValidClusterConfig() {
    return ProgramConfiguration.ClusterConfig.newBuilder()
        .addSubjectGroup(createValidSubjectGroupConfig())
        .setCluster(CLUSTER_NAME)
        .build();
  }

  private ProgramConfiguration.ClusterConfig.SubjectGroupConfig createValidSubjectGroupConfig() {
    return ProgramConfiguration.ClusterConfig.SubjectGroupConfig.newBuilder()
        .setSubjectGroupName(SUBJECT_GROUP_NAME)
        .setUser(USER)
        .setExpSettingsFilesDir(EXP_SETTINGS_FILES_DIR_1)
        .setNumberOfSubjects(SUBJECT_COUNT)
        .setSubjectWarmupTimeout(SUBJECT_WARMUP_TIMEOUT)
        .setNumberOfDefaultSubjects(SUBJECT_COUNT_WITH_DEFAULT_SETTINGS)
        .build();
  }

  private void verifyTinyValidConfig(ProtoBufConfig config) {
    assertNotNull(config.getJvmSearchSpaceRestriction());

    assertEquals("too many clusters in clusters list", 1, config.getClusterConfigs().size());
    ProtoBufConfig.ProtoBufClusterConfig cluster = config.getClusterConfig(CLUSTER_NAME);
    assertNotNull("failed to find the cluster by name", cluster);
    assertEquals("cluster name", CLUSTER_NAME, cluster.getName());
    assertEquals("cluster parent", config, cluster.getParentConfig());

    assertEquals("too many groups in group list", 1, cluster.getSubjectGroupConfigs().size());
    ProtoBufConfig.ProtoSubjectGroupConfig group =
        cluster.getSubjectGroupConfig(SUBJECT_GROUP_NAME);
    assertNotNull("failed to find the group by name", group);
    assertEquals("subject group", SUBJECT_GROUP_NAME, group.getName());
    assertEquals("user", USER, group.getUser());
    assertEquals("args_files_dir", EXP_SETTINGS_FILES_DIR_1, group.getExperimentSettingsFilesDir());
    assertEquals("subject group parent", cluster, group.getParentCluster());
    assertEquals("subject list", 0, group.getSpecialSubjectConfigs().size());
  }
}
