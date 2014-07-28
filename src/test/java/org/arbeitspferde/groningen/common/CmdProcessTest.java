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

package org.arbeitspferde.groningen.common;


import org.arbeitspferde.groningen.externalprocess.CmdProcessInvoker;
import org.arbeitspferde.groningen.security.open.NullSecurityManager;
import org.arbeitspferde.groningen.utility.MetricExporter;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.io.BufferedReader;

/**
 * The test for {@link CmdProcess}.
 */
public class CmdProcessTest extends TestCase {
  public void testCmdProcess() throws Exception {
    final MetricExporter mockMetricExporter = EasyMock.createNiceMock(MetricExporter.class);
    final CmdProcessInvoker cmdProcess =
        new CmdProcessInvoker(mockMetricExporter, new NullSecurityManager());

    BufferedReader stdout = cmdProcess.invokeAndWait(new String[] {"ls", "-l"}, -1);
    String line = stdout.readLine();

    Assert.assertTrue(line.length() > 0);
  }

}
