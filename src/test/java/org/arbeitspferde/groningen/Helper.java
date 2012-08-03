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
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * {@link Helper} is a simple class that assists with some basic testing functions that may
 * be used by a plethora of test cases.
 */
public class Helper {
  private static final Logger log = Logger.getLogger(Helper.class.getCanonicalName());

  /**
   * Provision an appropriate location for testing given the test harness.
   *
   * It is incumbent on the user of this method to clean up after its emissions.
   *
   * @return A {@link File} where temporary files will be created.
   * @throws IOException In case no test directory can be created nor found.
   */
  public static File getTestDirectory() throws IOException {
    final String[] testDirectories = new String[] {
        System.getenv("TEST_TMPDIR"),
        System.getProperty("java.io.tmpdir"),
        File.createTempFile("testing",
          String.format("%s.%s", System.getenv("USER"), System.nanoTime())).getAbsolutePath()};


    for (final String candidate : testDirectories) {
      log.info(String.format("Testing %s", candidate));

      if (candidate == null) {
        continue;
      }

      /*
       * It may be the case that the test directory refers to /tmp or /home.  It would be dangerous
       * were that to get wiped out.
       */
      final String fullCandidate = String.format("%s/runtime", candidate, candidate);

      final File candidateFile = new File(fullCandidate);

      if (candidateFile.exists()) {
        return candidateFile;
      }

      if (candidateFile.mkdir()) {
        return candidateFile;
      } else {
        log.severe(String.format(
            "Could not create temporary directory: %s.", candidateFile.getAbsoluteFile()));
      }
    }

    throw new IOException("Coult not acquire nor create a test directory.");
  }
}
