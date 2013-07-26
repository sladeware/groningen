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

package org.arbeitspferde.groningen.subject.open;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.subject.ServingAddressGenerator;
import org.arbeitspferde.groningen.subject.SubjectGroup;
import org.arbeitspferde.groningen.utility.Process;

import java.util.List;

/**
 * Generates process unique address.
 */
@Singleton
public class ProcessServingAddressGenerator implements ServingAddressGenerator {
  private static final Joiner slashJoiner = Joiner.on("/");

  public static final class ProcessServingAddressInfo {
    private String processClusterName;
    private String processUserName;
    private String processGroupName;
    private Integer processId;

    public ProcessServingAddressInfo(String processClusterName, String processUserName,
        String processGroupName, int processId) {
      this.processClusterName = processClusterName;
      this.processUserName = processUserName;
      this.processGroupName = processGroupName;
      this.processId = processId;
    }

    public String getProcessUserName() {
      return processUserName;
    }

    public String getProcessGroupName() {
      return processGroupName;
    }

    public Integer getProcessId() {
      return processId;
    }
  }

  public static ProcessServingAddressInfo parseAddress(final String address) {
    int processId;
    String[] addressParts = Iterables.toArray(Splitter.on("/").split(address), String.class);
    try {
      processId = Integer.parseInt(addressParts[addressParts.length - 1]);
    } catch (NumberFormatException e) {
      return null;
    }
    return new ProcessServingAddressInfo(addressParts[0], addressParts[1], addressParts[2],
        processId);
  }

  /**
   * Returns process serving address that has the following format:
   * //CLUSTER_NAME/USER_NAME/GROUP_NAME/PID
   *
   * @param group the subject group of the process.
   * @param index the index of the process in the group.
   */
  @Override
  public String addressFor(final SubjectGroup group, final int index) {
    List<Integer> processIds = null;
    try {
      int processGroupId = Integer.parseInt(group.getSubjectGroupName());
      processIds = Process.getProcessIdsOf(processGroupId);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Named process groups are not supported yet.");
    }
    return slashJoiner.join("/", group.getClusterName(), group.getUserName(),
        group.getSubjectGroupName(), processIds.get(index).toString());
  }
}