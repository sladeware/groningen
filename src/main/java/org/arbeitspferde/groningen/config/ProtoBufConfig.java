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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.JvmSearchSpace;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@code GroningenConfig} constructed from a protocol buffer.
 *
 * <p>Does not support in-place updating (i.e. tracking of changes to the underlying
 * configuration data).
 *
 * <p>NOTE: the {@link GroningenConfig.ClusterConfig}, {@link GroningenConfig.SubjectGroupConfig}, and
 * {@link GroningenConfig.SubjectConfig} subinterfaces are implemented as inner classes here as they
 * depend heavily on the protobuf hierarchy and thus it made sense to keep them together. The
 * other more generic implementations are split out into other class files.
 */
public class ProtoBufConfig implements GroningenConfig {
  // construction factory for objects outside the scope of this class, allows for mocks to be
  // injected
  private static ProtoBufConfigConstructorFactory constructorFactory =
      new ProtoBufConfigConstructorFactory();

  // reference to the top level protobuffer config
  private final ProgramConfiguration protoConfig;

  // map of cluster names to the cluster. Immutable since we don't allow changes to the list
  // we require there to be clusters and since it is immutable, it is also marked final.
  private final ImmutableMap<String, ProtoBufClusterConfig> protoClusterConfigs;

  // cached list of the available clusters
  private final ImmutableList<ClusterConfig> clustersList;

  // optional jvm search space range restrictions
  private GenericSearchSpaceBundle searchSpaceRestriction = null;

  /**
   * Replace the factory to be used to construct classes external to this file with another
   * instance.
   *
   * @param factory new factory object to use when generating objects outside of this class or
   *     its nested classes
   */
  @VisibleForTesting
  static void setConstructorFactory(ProtoBufConfigConstructorFactory factory) {
    constructorFactory = factory;
  }

  /**
   * Verifies an expression is true and throws an exception if it is not.
   *
   * @param expr expression that must be true
   * @param message message to include with the exception if {@code expr} is not true
   * @throws InvalidConfigurationException thrown when {@code expr} is false
   */
  private static void validateItem(boolean expr, String message)
      throws InvalidConfigurationException {
    if (!expr) {
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Creates this object from the protocol buffer representation of the configuration essentially
   * gluing the protocol buffer to the {@code GroningenConfig} interface. The constructor validates
   * the fields as it goes such that after successful construction, the object will be valid and
   * fully specified.
   *
   * @param progConfig the protocol buffer representation of the overall configuration
   * @throws InvalidConfigurationException invalid or incomplete configuration was specified
   */
  public ProtoBufConfig(ProgramConfiguration progConfig)
      throws InvalidConfigurationException {

    protoConfig = Preconditions.checkNotNull(progConfig);

    /*
     * Traverse the configuration tree contained within the protocol buffer. Since separating
     * validation and instantiation would require two passes through the tree (and validation
     * of the children of the root would need be validated via a mechanism like static methods)
     * and because configuration churn is expected to very light, do validation and instantiation
     * on the same pass through the tree.
     *
     * Start with verifying all required globals without defaults have values defined.
     */
    validateItem(progConfig.hasParamBlock(), "GroningenParam message omitted in supplied proto");
    validateItem(
        progConfig.hasParamBlock(), "GroningenParam message omitted in supplied proto");
    validateParamBlock(progConfig);

    // pull in the optional search space restriction which can still throw an exception on
    // instantiation
    if (progConfig.hasJvmSearchRestriction()) {
      searchSpaceRestriction =
          constructorFactory.createProtoBufSearchSpaceBundle(
              progConfig.getJvmSearchRestriction(), false);
    } else {
      searchSpaceRestriction = new GenericSearchSpaceBundle();
    }

    // traverse the cluster definitions - we need at least one to have a valid experiment
    validateItem(progConfig.getClusterCount() > 0, "proto definition lacked any clusters");
    ImmutableMap.Builder<String, ProtoBufClusterConfig> buildCluster = ImmutableMap.builder();
    for (ProgramConfiguration.ClusterConfig protoCluster : progConfig.getClusterList()) {
      ProtoBufClusterConfig cluster = new ProtoBufClusterConfig(protoCluster, this);
      buildCluster.put(cluster.getName(), cluster);
    }

    try {
      protoClusterConfigs = buildCluster.build();
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigurationException("duplicate cluster names were included: " + e);
    }

    clustersList = ImmutableList.<ClusterConfig>copyOf(protoClusterConfigs.values());
  }

  /**
   * Validates that all fields in the param block that do not have default values have
   * values supplied.
   */
  private void validateParamBlock(ProgramConfiguration progConfig)
      throws InvalidConfigurationException {
    List<String> missingParams = Lists.newLinkedList();
    GroningenParams protoParams = progConfig.getParamBlock();
    if (!protoParams.hasInputLogName()) {
      missingParams.add("input_log_name");
    }

    if (missingParams.size() > 0) {
      Joiner joiner = Joiner.on(", ");
      throw new InvalidConfigurationException(
          "missing global vars: " + joiner.join(missingParams));
    }
  }

  @Override
  public ProtoBufClusterConfig getClusterConfig(String clusterName) {
    return protoClusterConfigs.get(clusterName);
  }

  @Override
  public ImmutableList<ClusterConfig> getClusterConfigs() {
    return clustersList;
  }

  @Override
  public SearchSpaceBundle getJvmSearchSpaceRestriction() {
    return searchSpaceRestriction;
  }

  @Override
  public GroningenParams getParamBlock() {
    return protoConfig.getParamBlock();
  }

  @Override
  public ProgramConfiguration getProtoConfig() {
    return protoConfig;
  }

  /**
   * Retrieve the user.
   *
   * @return the user entry if it exists (and even if it is null), otherwise null
   */
  @Nullable
  @VisibleForTesting
  String retrieveFirstUser() {
    return protoConfig.hasUser() ? protoConfig.getUser() : null;
  }

  /**
   * Retrieve the numberOfSubject.
   *
   * @return the numberOfSubject entry which should have a default value.
   */
  @VisibleForTesting
  int retrieveFirstNumberOfSubjects() {
    return protoConfig.getNumberOfSubjects();
  }

  /**
   * Retrieve the subjectWarmupTimeout.
   *
   * @return the subjectWarmupTimeout entry.
   */
  @VisibleForTesting
  int retrieveFirstSubjectWarmupTimeout() {
    return protoConfig.getSubjectWarmupTimeout();
  }

  /**
   * A {@link ClusterConfig} that wraps a protobuf based configuration containing all cluster,
   * subject group and subject settings.
   */
  @VisibleForTesting
  static class ProtoBufClusterConfig implements ClusterConfig {
    // backpointer to the overall config for allowing access to the top level proto
    private final ProtoBufConfig parent;

    // the proto for this cluster which contains handy things like the cluster name
    private final ProgramConfiguration.ClusterConfig protoClusterConfig;

    // map of the jobs contained herein
    private final ImmutableMap<String, ProtoSubjectGroupConfig> protosSubjectGroupConfigs;

    // cached list of the available jobs
    private final ImmutableList<SubjectGroupConfig> subjectGroupList;

    /**
     * Constructor for the cluster and everything below it in the configuration tree. Does validation
     * that required fields are included within the protobuf.
     *
     * @param protoConfig the protobuf class representing the cluster
     * @param parent backpointer to the {@code ProtoBufConfig} of which this cluster is a part
     * @throws InvalidConfigurationException required fields were either omitted or had values
     *     outside of acceptable ranges
     */
    public ProtoBufClusterConfig(ProgramConfiguration.ClusterConfig protoConfig,
                              ProtoBufConfig parent) throws InvalidConfigurationException {
      this.parent = checkNotNull(parent);
      this.protoClusterConfig = checkNotNull(protoConfig);

      // at this level, required verification is only that a cluster name and some jobs are present
      validateItem(protoConfig.hasCluster(), "required cluster name was missing");

      // finally make the jobs perform verification as each is instantiated
      validateItem(
          protoConfig.getSubjectGroupCount() > 0, "proto definition lacked any subject group");
      ImmutableMap.Builder<String, ProtoSubjectGroupConfig> builder = ImmutableMap.builder();
      for (ProgramConfiguration.ClusterConfig.SubjectGroupConfig subjectGroupProto : protoConfig.getSubjectGroupList()) {
        ProtoSubjectGroupConfig job = new ProtoSubjectGroupConfig(subjectGroupProto, this);
        builder.put(job.getName(), job);
      }

      try {
        protosSubjectGroupConfigs = builder.build();
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            "duplicate job names were included for cluster " + protoConfig.getCluster() + ": " + e);
      }

      subjectGroupList =
          ImmutableList.<SubjectGroupConfig>copyOf(protosSubjectGroupConfigs.values());
    }

    @Override
    public ProtoSubjectGroupConfig getSubjectGroupConfig(final String subjectGroupName) {
      return protosSubjectGroupConfigs.get(subjectGroupName);
    }

    /**
     * {@inheritDoc}
     *
     * Will not be null.
     */
    @Override
    public ImmutableList<SubjectGroupConfig> getSubjectGroupConfigs() {
      return subjectGroupList;
    }

    /**
     * {@inheritDoc}
     *
     * Will not be null.
     */
    @Override
    public String getName() {
      return protoClusterConfig.getCluster();
    }

    /**
     * {@inheritDoc}
     *
     * Will not be null.
     */
    @Override
    public ProtoBufConfig getParentConfig() {
      return parent;
    }

    @VisibleForTesting
    ProgramConfiguration.ClusterConfig getProtoClusterConfig() {
      return protoClusterConfig;
    }

    /**
     * Retrieve the first definition of the user in the configuration tree starting at the
     * cluster level and looking upward.
     *
     * @return the first user entry found even if this value is null, null if no user
     *     fields were found in the hierarchy.
     */
    @Nullable
    @VisibleForTesting
    String retrieveFirstUser() {
      return protoClusterConfig.hasUser()
          ? protoClusterConfig.getUser() : parent.retrieveFirstUser();
    }

    /**
     * Retrieve the first definition of the numberOfSubjects in the configuration tree starting
     * at the cluster level and looking upward.
     *
     * @return the first number of subjects entry found in the hierarchy
     */
    @Nullable
    @VisibleForTesting
    int retrieveFirstNumberOfSubjects() {
      return protoClusterConfig.hasNumberOfSubjects()
          ? protoClusterConfig.getNumberOfSubjects() : parent.retrieveFirstNumberOfSubjects();
    }

    /**
     * Retrieve the first definition of the subjectWarmupTimeout in the configuration tree starting
     * at the cluster level and looking upward.
     *
     * @return the first subject warmup timeout entry found in the hierarchy
     */
    @Nullable
    @VisibleForTesting
    int retrieveFirstSubjectWarmupTimeout() {
      return protoClusterConfig.hasSubjectWarmupTimeout()
          ? protoClusterConfig.getSubjectWarmupTimeout() :
            parent.retrieveFirstSubjectWarmupTimeout();
    }
  }

  /**
   * The job to be experimented upon, some parts of which can be described on a per subject
   * basis.
   */
  @VisibleForTesting
  static class ProtoSubjectGroupConfig implements SubjectGroupConfig {
    // backpointer to the cluster in which this subject group exists
    private final ProtoBufClusterConfig parentCluster;

    // reference to the underlying protobuf describing the subject group
    private final ProgramConfiguration.ClusterConfig.SubjectGroupConfig protoSubjectGroupConfig;

    // List of any subject-specific configuration
    private final ImmutableList<SubjectConfig> subjectConfigs;

    // Path where per-subject experiment settings for this job will be stored
    private final String expSettingsFilesDir;

    // user to run as
    private final String user;

    // number of subjects in the subject group that we control
    private final int numberOfSubjects;

    // max seconds a subject is allowed to be unhealthy
    private final int subjectWarmupTimeout;

    /**
     * Construct and validate a subject group level (and lower) configuration.
     *
     * @param protoSubjectGroup the protobuf class representing the subject group
     * @param parent backpointer to the {@link ProtoBufClusterConfig} in which this subject group
     *               is running
     * @throws InvalidConfigurationException required fields were either omitted or had values
     *     outside of acceptable ranges
     */
    public ProtoSubjectGroupConfig(
        final ProgramConfiguration.ClusterConfig.SubjectGroupConfig protoSubjectGroup,
        final ProtoBufClusterConfig parent) throws InvalidConfigurationException {
      this.parentCluster = checkNotNull(parent);
      this.protoSubjectGroupConfig = checkNotNull(protoSubjectGroup);

      // do verification after we save off the protoConfig and parent so the find methods
      // have what they need to work with
      validateItem(protoSubjectGroup.hasSubjectGroupName(),
          "required subject_group_name was missing");
      String subjectGroupName = protoSubjectGroup.getSubjectGroupName();
      validateItem(protoSubjectGroup.hasExpSettingsFilesDir(),
          "required exp_settings_files_dir was missing");
      validateItem(protoSubjectGroup.getExpSettingsFilesDir().startsWith("/"),
          "exp_settings_files_dir should start with /");
      expSettingsFilesDir = protoSubjectGroup.getExpSettingsFilesDir();

      user = retrieveFirstUser();
      validateItem(user != null,
          "user must be specified as nonnull in config hierarchy for job " + subjectGroupName);

      numberOfSubjects = retrieveFirstNumberOfSubjects();
      validateItem(numberOfSubjects >= 0,
          "numberOfSubjects must be non-negative. 0 means use all subjects in the job");

      subjectWarmupTimeout = retrieveFirstSubjectWarmupTimeout();
      validateItem(subjectWarmupTimeout >= 1,
          "subjectWarmupTimeout must be positive. The vaule is in seconds.");

      // lastly make up any special subject configs verifying as we go - nothing here makes use of
      // the ProtoBufSubjectConfig so we can safely store as a list of {@code SubjectConfig}
      ImmutableList.Builder<SubjectConfig> builder = ImmutableList.builder();
      for (ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig subject :
           protoSubjectGroupConfig.getSubjectConfigList()) {
        builder.add(new ProtoBufSubjectConfig(subject, this));
      }
      subjectConfigs = builder.build();
    }

    /**
     * {@inheritDoc}
     *
     * Will not return null.
     */
    @Override
    public String getExperimentSettingsFilesDir() {
      return expSettingsFilesDir;
    }

    /**
     * {@inheritDoc}
     *
     * Will not return null.
     */
    @Override
    public String getName() {
      return protoSubjectGroupConfig.getSubjectGroupName();
    }

    /**
     * {@inheritDoc}
     *
     * Will not return null.
     */
    @Override
    public ImmutableList<SubjectConfig> getSpecialSubjectConfigs() {
      return subjectConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * will not return null.
     */
    @Override
    public String getUser() {
      return user;
    }

    /**
     * {@inheritDoc}
     *
     * will not return null.
     */
    @Override
    public int getNumberOfSubjects() {
      return numberOfSubjects;
    }

    /**
     * {@inheritDoc}
     *
     * will not return null.
     */
    @Override
    public int getSubjectWarmupTimeout() {
      return subjectWarmupTimeout;
    }

    /**
     * {@inheritDoc}
     *
     * will not return null.
     */
    @Override
    public ClusterConfig getParentCluster() {
      return parentCluster;
    }

    @Override
    public boolean hasRestartCommand() {
      return protoSubjectGroupConfig.getRestartCommandList().size() > 0;
    }

    @Override
    public String[] getRestartCommand() {
      return protoSubjectGroupConfig.getRestartCommandList().toArray(new String[0]);
    }

    @VisibleForTesting
    ProgramConfiguration.ClusterConfig.SubjectGroupConfig getProtoSubjectGroupConfig() {
      return protoSubjectGroupConfig;
    }

    /**
     * Retrieve the first definition of the user in the configuration tree starting at the
     * subject group level and looking upward.
     *
     * @return the first user entry found even if this value is null, null if no user
     *     fields were found in the hierarchy.
     */
    @Nullable
    @VisibleForTesting
    String retrieveFirstUser() {
      return protoSubjectGroupConfig.hasUser()
          ? protoSubjectGroupConfig.getUser() : parentCluster.retrieveFirstUser();
    }

    /**
     * Retrieve the first definition of the numberOfSubjects in the configuration tree starting at
     * the subject group level and looking upward.
     *
     * @return the first numberOfSubjects entry found even if this value is null, null if no
     *     numberOfSubjects fields were found in the hierarchy.
     */
    @Nullable
    @VisibleForTesting
    int retrieveFirstNumberOfSubjects() {
      return protoSubjectGroupConfig.hasNumberOfSubjects()
          ? protoSubjectGroupConfig.getNumberOfSubjects() :
            parentCluster.retrieveFirstNumberOfSubjects();
    }

    /**
     * Retrieve the first definition of the subjectWarmupTimeout in the configuration tree
     * starting at the subject group level and looking upward.
     *
     * @return the first subjectWarmupTimeout entry found even if this value is null, null if no
     *     subjectWarmupTimeout fields were found in the hierarchy.
     */
    @Nullable
    @VisibleForTesting
    int retrieveFirstSubjectWarmupTimeout() {
      return protoSubjectGroupConfig.hasSubjectWarmupTimeout()
          ? protoSubjectGroupConfig.getSubjectWarmupTimeout() :
            parentCluster.retrieveFirstSubjectWarmupTimeout();
    }
  }

  /**
   * Subject specific configuration.
   */
  @VisibleForTesting
  static class ProtoBufSubjectConfig implements SubjectConfig {

    protected SubjectGroupConfig parent = null;

    // Null represents no setting
    @Nullable protected Integer subjectIndex = null;

    // This is a strictly defined set of parameters and as such all should be defined
    @Nullable protected ProtoBufSearchSpaceBundle jvmParams = null;

    /**
     * Translate the protobuf's SubjectConfig representation.
     *
     * @param subjectConfig the protobuf representation of the specified subject config
     * @param parent backpointer to the {@code JobConfig} and its parents
     * @throws InvalidConfigurationException fields were malformed
     */
    public ProtoBufSubjectConfig(
        ProgramConfiguration.ClusterConfig.SubjectGroupConfig.ExtendedSubjectConfig subjectConfig,
        SubjectGroupConfig parent) throws InvalidConfigurationException {

      // blindly save off the pointer to the parent
      this.parent = parent;

      if (subjectConfig.hasJvmParameters()) {
        jvmParams =
            constructorFactory.createProtoBufSearchSpaceBundle(
                subjectConfig.getJvmParameters(), true);
      }

      if (subjectConfig.hasSubjectIndex()) {
        // verification
        // the only thing I can think to check is that the subjectIndices are natural numbers
        validateItem(subjectConfig.getSubjectIndex() >= 0,
            "negative subjectIndex supplied: " + subjectConfig.getSubjectIndex());
        subjectIndex = Integer.valueOf(subjectConfig.getSubjectIndex());
      }
    }

    @Override
    @Nullable
    public SearchSpaceBundle getJvmSearchSpaceDefinition() {
      return jvmParams;
    }

    @Override
    @Nullable
    public Integer getSubjectIndex() {
      return subjectIndex;
    }

    @Override
    public SubjectGroupConfig getParentSubjectGroup() {
      return parent;
    }
  }

  /**
   * Factory for objects external to the classes contained in this file.
   */
  @VisibleForTesting
  static class ProtoBufConfigConstructorFactory {
    /**
     * Constructs a {@code ProtoBufSearchSpaceBundle}. See the replaced
     * {@link ProtoBufSearchSpaceBundle#ProtoBufSearchSpaceBundle(JvmSearchSpace, boolean)
     * constructor} for details.
     */
    public ProtoBufSearchSpaceBundle createProtoBufSearchSpaceBundle(JvmSearchSpace proto,
        boolean requireAll) throws InvalidConfigurationException {
      return new ProtoBufSearchSpaceBundle(proto, requireAll);
    }

  }
}
