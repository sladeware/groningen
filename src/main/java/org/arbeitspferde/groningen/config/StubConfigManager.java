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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Stub for enabling a ConfigManagerProvider system. Classes defined here have a generic mutator
 * which sets fields via reflection in addition to the accessors so that return values can be set.
 * The mutator is static in the top level class and therefore requires that member vars are
 * protected and not private.
 */
public class StubConfigManager implements ConfigManager {

  /**
   * Generic mutator.
   *
   * @param dest an {@link Object} which should be mutated
   * @param varName name of the instance var (remove the get from the accessor and lowercase the new
   *     first letter) as a {@link String}
   * @param newValue an {@link Object} representing the new value. primitive types will need to be
   *     passed in as their Object counterparts
   * @return true iff the set was successful, false if an exception was thrown during modification
   */
  public static boolean set(Object dest, String varName, Object newValue) {
    boolean ret = true;

    // errors are swallowed as this expected to only be used during development
    // and testing
    try {
      Field instanceVar = dest.getClass().getDeclaredField(varName);
      instanceVar.set(dest, newValue);
    } catch (IllegalAccessException e) {
      ret = false;
    } catch (IllegalArgumentException e) {
      ret = false;
    } catch (SecurityException e) {
      ret = false;
    } catch (NoSuchFieldException e) {
      ret = false;
    }

    return ret;
  }

  /**
   * @see org.arbeitspferde.groningen.config.ConfigManager#initialize()
   */
  @Override
  public void initialize() {
    // nothing to do
  }

  /**
   * @see org.arbeitspferde.groningen.config.ConfigManager#queryConfig()
   */
  @Override
  public GroningenConfig queryConfig() {
    return null;
  }

  /**
   * Stub Config class that can be modified
   *
   */
  public static class StubConfig implements GroningenConfig {

    // Instance vars map to the getters in GroningenConfig
    protected Map<String, ClusterConfig> clusterConfigs = Maps.newHashMap();
    protected SearchSpaceBundle searchSpaceBundle = null;
    protected GroningenParams paramBlock = null;

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig#getClusterConfig(String)
     */
    @Override
    public ClusterConfig getClusterConfig(String clusterName) {
      return clusterConfigs.get(clusterName);
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig#getClusterConfigs()
     */
    @Override
    public ImmutableList<ClusterConfig> getClusterConfigs() {
      return ImmutableList.copyOf(clusterConfigs.values());
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig#getJvmSearchSpaceRestriction()
     */
    @Override
    public SearchSpaceBundle getJvmSearchSpaceRestriction() {
      return searchSpaceBundle;
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig#getParamBlock()
     */
    @Override
    public GroningenParamsOrBuilder getParamBlock() {
      return paramBlock;
    }

    @Override
    public ProgramConfiguration getProtoConfig() {
      throw new RuntimeException("Not implemented.");
    }

  }

  /**
   * Stub implementation of ClusterConfig
   *
   * Use the mutator in StubConfigManager
   *
   */
  public static class StubClusterConfig implements ClusterConfig {
    @Nullable
    protected GroningenConfig parentConfig = null;
    protected Map<String, SubjectGroupConfig> groupConfigs = Maps.newHashMap();
    protected String name = null;

    /**
     * @see GroningenConfig.ClusterConfig#getSubjectGroupConfig(String)
     */
    @Override
    public SubjectGroupConfig getSubjectGroupConfig(String name) {
      return groupConfigs.get(name);
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig#getSubjectGroupConfigs()
     */
    @Override
    public ImmutableList<SubjectGroupConfig> getSubjectGroupConfigs() {
      return ImmutableList.copyOf(groupConfigs.values());
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig#getName()
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * @see org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig#getParentConfig()
     */
    @Override
    public GroningenConfig getParentConfig() {
      return parentConfig;
    }
  }

  /**
   * Stub implementation of SubjectGroupConfig
   *
   * Use the mutator in StubConfigManager
   *
   */
  public static class StubGroupConfig implements SubjectGroupConfig {
    protected ClusterConfig parentCluster = null;
    protected String expSettingsFilesDir = null;
    protected String name = null;
    protected String user = null;
    protected List<String> restartCommand = Lists.newArrayList();
    protected List<SubjectConfig> specialSubjectConfigs = Lists.newArrayList();
    protected int numberOfSubjects = 0;
    protected int subjectWarmupTimeout = 0;

    /**
     * @see SubjectGroupConfig#getName()
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * @see SubjectGroupConfig#getSpecialSubjectConfigs()
     */
    @Override
    public ImmutableList<SubjectConfig> getSpecialSubjectConfigs() {
      return ImmutableList.copyOf(specialSubjectConfigs);
    }

    /**
     * @see SubjectGroupConfig#getUser()
     */
    @Override
    public String getUser() {
      return user;
    }

    /**
     * @see SubjectGroupConfig#getParentCluster()
     */
    @Override
    public ClusterConfig getParentCluster() {
      return parentCluster;
    }

    @Override
    public int getNumberOfSubjects() {
      return numberOfSubjects;
    }

    @Override
    public int getSubjectWarmupTimeout() {
      return subjectWarmupTimeout;
    }

    @Override
    public String getExperimentSettingsFilesDir() {
      return expSettingsFilesDir;
    }

    /**
     * Set the restart command.
     */
    public void setRestartCommand(List<String> newCommand) {
      restartCommand = newCommand;
    }

    @Override
    public boolean hasRestartCommand() {
      return restartCommand.size() > 0;
    }

    @Override
    public String[] getRestartCommand() {
      return restartCommand.toArray(new String[0]);
    }
  }

  /**
   * Stub implementation of SubjectConfig
   *
   * Use the mutator in StubConfigManager
   *
   */
  public static class StubSubjectConfig implements SubjectConfig {
    protected SubjectGroupConfig parentGroup = null;
    protected SearchSpaceBundle jvmSearchSpaceDefinition = null;
    protected int subjectIndex = -1;

    /**
     * @see SubjectConfig#getJvmSearchSpaceDefinition()
     */
    @Override
    public SearchSpaceBundle getJvmSearchSpaceDefinition() {
      return jvmSearchSpaceDefinition;
    }

    /**
     * @see SubjectConfig#getSubjectIndex()
     */
    @Override
    public Integer getSubjectIndex() {
      return subjectIndex;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.perftools.jtune.config.GroningenConfig.SubjectConfig#getParentGroup()
     */
    @Override
    public SubjectGroupConfig getParentSubjectGroup() {
      return parentGroup;
    }

  }

  /**
   * Stub implementation of {@link SearchSpaceBundle} which allows the user to
   * generate an array of {@link SearchSpaceBundle.SearchSpaceEntry
   * SearchSpaceEntry.SearchSpaceEntrys} which can then be retrieved. A stub
   * implementation of {@link SearchSpaceBundle.SearchSpaceEntry} can be found
   * at {@link StubSearchSpaceEntry}.
   *
   * The reflection based mutator above can be used to set the searchSpaces
   * which is an array of {@link SearchSpaceBundle.SearchSpaceEntry}
   */
  public static class StubSearchSpaceBundle implements SearchSpaceBundle {

    protected SearchSpaceEntry[] searchSpaces = new SearchSpaceEntry[JvmFlag.values().length];

    /**
     * Retrieve a specific CommandLineArgument from the current searchSpaces
     * array. You will want to set that array before using this method
     */
    @Override
    public SearchSpaceEntry getSearchSpace(JvmFlag arg) {
      if (arg.ordinal() >= searchSpaces.length) {
        throw new IllegalArgumentException("CommandLineArgument outside expected range");
      }
      return searchSpaces[arg.ordinal()];
    }

    /**
     * Retrieve a copy of the searchSpaces array. You will want to set that
     * array before using this method.
     */
    @Override
    public SearchSpaceEntry[] getSearchSpaces() {
      return searchSpaces.clone();
    }
  }

  /**
   * Bean for holding SearchSpaceEntry information
   *
   * Constructor sets but instance vars can be reset using reflection based
   * mutator above
   */
  public static class StubSearchSpaceEntry implements SearchSpaceBundle.SearchSpaceEntry {
    protected JvmFlag representedArg = null;
    protected Long floor = Long.MAX_VALUE;
    protected Long ceiling = Long.MIN_VALUE;
    protected Long stepSize = Long.MIN_VALUE;

    public StubSearchSpaceEntry(JvmFlag arg, long floor, long ceiling, long step) {
      representedArg = arg;
      this.floor = floor;
      this.ceiling = ceiling;
      this.stepSize = step;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getRepresentedArg()
     */
    @Override
    public JvmFlag getRepresentedArg() {
      return representedArg;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getFloor()
     */
    @Override
    public long getFloor() {
      return floor;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getCeiling()
     */
    @Override
    public long getCeiling() {
      return ceiling;
    }

    /**
     * @see SearchSpaceBundle.SearchSpaceEntry#getStepSize()
     */
    @Override
    public long getStepSize() {
      return stepSize;
    }
  }

  /**
   * @see ConfigManager#shutdown()
   */
  @Override
  public void shutdown() {

  }
}
