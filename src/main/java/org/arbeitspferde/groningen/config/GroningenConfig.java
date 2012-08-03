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

import com.google.common.collect.ImmutableList;

import org.arbeitspferde.groningen.config.StubConfigManager.StubConfig;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;

import javax.annotation.Nullable;

/**
 * This interface represents all of the configuration data for one running
 * instance of Groningen.
 */
public interface GroningenConfig {

  /**
   * Returns the {@link ClusterConfig} with the specified name
   *
   * @param clusterName {@link String} representation of the cluster name for which to retrieve the
   * {@link ClusterConfig}
   * @return A {@link ClusterConfig} if the cluster is present in the experiment, null otherwise
   */
  public ClusterConfig getClusterConfig(String clusterName);

  /**
   * Returns a {@link ImmutableList} of {@link ClusterConfig}, one for each cluster
   * in which this Groningen experiment is running.
   *
   * @return A {@link ImmutableList} of {@link ClusterConfig}
   */
  public ImmutableList<ClusterConfig> getClusterConfigs();

  /**
   * Retrieve the limitations placed on the search space. These can map to single values or ranges.
   *
   * @return the search space container defining the restrictions on the space
   */
  public SearchSpaceBundle getJvmSearchSpaceRestriction();

  /**
   * Other Experiment wide options to be specified in the configuration file
   *
   * @return the {@link GroningenParams} protobuf with general params set
   */
  public GroningenParamsOrBuilder getParamBlock();

  /**
   * <p>
   * The use of the {@link GroningenConfig} interface to capture to current operating parameters for
   * Groningen may be poor. All concrete classes hereof (i.e., {@link ProtoBufConfig} and
   * {@link StubConfig}) lack business logic and are effectively dumb data objects that wrap around
   * configuration-specific implementation needs.
   * </p>
   *
   * <p>
   * This seems suboptimal, because we need to embed a Protocol Buffer serialized-version of the
   * configuration into our event logs regardless of the underlying configuration methodology, which
   * means ultimately a Protocol Buffer will need to be constructed. With this in mind, I propose we
   * deprecate {@link GroningenConfig}, {@link ProtoBufConfig}, and {@link StubConfig} and replace
   * them with a canonical <em>immutable</em> Protocol Buffer DDO to manage Groningen operation.
   * This Protocol Buffer, perhaps, should be the same as {@link ProgramConfiguration} due to its
   * vetted design and should be constructed via a configuration-methodology-specific factory.
   * </p>
   *
   * <p>
   * This route is much simpler than having any implementation-specific configuration mechanism
   * other than the existing {@link ProtoBufConfig} (e.g., {@link StubConfig}) serialize their state
   * back into a {@link ProgramConfiguration}.
   * </p>
   *
   * <p>
   * If the aforementioned is desired, we should mark the noted classes and their related ones as
   * deprecated to prepare ourselves.
   * </p>
   *
   * @returns The Protocol Buffer version of this configuration here for purposes of embedding into
   *          event log emissions.
   */
  @Nullable
  ProgramConfiguration getProtoConfig();

  /**
   * A {@link ClusterConfig} represents the Groningen configuration data about a single
   * cluster (a.k.a data center)
   */
  public interface ClusterConfig {

    /**
     * Returns the {@link GroningenConfig} of which this ClusterConfig is a child.
     *
     * @return {@link GroningenConfig} of which this ClusterConfig is a child. Can be null if used
     * outside the GroningenConfig config hierarchy
     */
    @Nullable
    public GroningenConfig getParentConfig();

    /**
     * Returns the name of the cluster
     *
     * @return The name of the cluster
     */
    public String getName();

    /**
     * Returns a {@link SubjectGroupConfig} with the given name within this cluster that is
     * currently running as part of the Groningen experiment.
     *
     * @param subjectGroupName {@link String} representation of the subject group name for which to
     * retrieve the {@link SubjectGroupConfig}.
     * @return A {@link SubjectGroupConfig} if one is part of the experiment in this cluster, null
     *         otherwise
     */
    @Nullable
    public SubjectGroupConfig getSubjectGroupConfig(String subjectGroupName);

    /**
     * Returns a {@link List} of {@link SubjectGroupConfig}, one for each subject group
     * in the cluster that is part of this Groningen experiment.
     *
     * @return A {@link List} of {@link SubjectGroupConfig}
     */
    public ImmutableList<SubjectGroupConfig> getSubjectGroupConfigs();
  }

  /**
   * A {@code SubjectGroupConfig} represents the Groningen configuration data about a single
   * subject group in a Groningen experiment
   *
   */
  public interface SubjectGroupConfig {

    /**
     * Returns the {@link ClusterConfig} of which this {@link SubjectGroupConfig} is a child.
     *
     * @return {@link ClusterConfig} of which this {@link SubjectGroupConfig} is a child.
     * Can be null if used outside the GroningenConfig config hierarchy
     */
    @Nullable
    public ClusterConfig getParentCluster();

    /**
     * Returns the name of the subject group.
     *
     * @return the name of the subject group.
     */
    public String getName();

    /**
     * Returns a {@link String} representing the user that should be used to access the
     * subject group.
     *
     * @return The name of user.
     * @throws NullPointerException no user was supplied in the configuration hierarchy
     */
    public String getUser();

    /**
     * Returns a {@link String} representing the path where per-subject experiment settings files
     * will be stored. These files will be read by jvm_arg_injector and it will inject their
     * contents into the experiments' subjects' command line. For every subject group's member,
     * Groningen will create a file with a full path looking like this:
     * <exp_settings_files_dir>/<subject_index>
     *
     * @return Full path to the experiments settings files directory for the subjects of this
     *         subject group
     * @throws NullPointerException no experiment settings files directory path was specified for
     *         this subject group
     */
    public String getExperimentSettingsFilesDir();

    /**
     * Returns a {@link List} of {@link SubjectConfig} representing either
     * configuration for all of the subjects or none. Groningen will use all subjects in a subject
     * group, requiring separation of the test instances from the nontest instances into separate
     * subject groups. If the list specifies a partial set of the subjects, the remaining subjects
     * will have values chosen at random. This allows for injecting known argument sets to see how
     * they fair with randomly chosen values.
     *
     * @return list of configurations which can be linked to specific or random subjects depending
     * on the internals of the SubjectConfig
     */
    public ImmutableList<SubjectConfig> getSpecialSubjectConfigs();

    /**
     * Returns an int that represents the number of subjects in a subject group that Groningen is
     * permitted to control for experimentation purposes. Setting this to value N implies that
     * subjects 0 to N-1 within a subject group are controlled by Groningen.
     */
    public int getNumberOfSubjects();

    /**
     * Returns the max number of seconds that an experimental subject is allowed to _not_ return OK
     * on health checks probes when running an experiment. Defaulting to 0 implies that Groningen
     * will pick a reasonable value for this (currently 300 secs).
     */
    public int getSubjectWarmupTimeout();

    /**
     * Returns True if this subject group has a custom restart command.  Otherwise, an
     * automatically-generated one should be used.
     */
    public boolean hasRestartCommand();

    /**
     * Returns the custom restart command if one is specified.
     */
    public String[] getRestartCommand();
  }

  /**
   * A {@code SubjectConfig} represents the GroningenConfiguration data about a single
   * subject in a Groningen experiment.
   */
  public interface SubjectConfig {

    /**
     * Returns the {@link SubjectGroupConfig} of which this SubjectConfig is a child.
     *
     * @return {@link SubjectGroupConfig} of which this SubjectConfig is a child. Can be null if
     * used utside the GroningenConfig config hierarchy
     */
    @Nullable
    public SubjectGroupConfig getParentSubjectGroup();

    /**
     * Retrieve the subject index to use if one is specified.
     * @see SubjectConfig
     *
     * @return an {@code Integer} if a specific subject is to be used, null if one should be chosen
     * at random
     */
    public Integer getSubjectIndex();

    /**
     * Retrieve the jvm arguments for this subject if they have been defined.
     *
     * @return null if not specified, ...
     */
    @Nullable
    public SearchSpaceBundle getJvmSearchSpaceDefinition();
  }
}
