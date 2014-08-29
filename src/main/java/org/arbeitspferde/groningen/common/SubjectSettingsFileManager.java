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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.proto.ExpArgFile.ExperimentArgs;
import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.FileFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/* TODO(team): Consider refactoring this such that it no longer makes an assumption that a file
 *             controls the settings when some other means—e.g., RPC, web service—could be used.
 */

/**
 * Wraps File and RecordIO and provides simple interface for experiment settings files creation and
 * removal operations.
 */
@Singleton
public class SubjectSettingsFileManager {
  /** Logger for this class */
  private static final Logger log =
      Logger.getLogger(SubjectSettingsFileManager.class.getCanonicalName());

  private final FileFactory fileFactory;

  @Inject
  public SubjectSettingsFileManager(final FileFactory fileFactory) {
    this.fileFactory = fileFactory;
  }

  /** Writes the input jvmSettings to the file defined by the fileName. Failures
   *  in writing to the file are logged and the exception is consumed w/in this method.
   *
   *  @param experimentArgs Experiment settings to be written.
   *  @param fileName Destination file name.
   */
  public void write(ExperimentArgs experimentArgs, String fileName) {
    OutputStream ors = null;
    try {
      final AbstractFile settingsFile = fileFactory.forFile(fileName, "w");
      ors = settingsFile.outputStreamFor();
      ors.write(experimentArgs.toByteArray());
    } catch (IOException e) {
      log.log(Level.SEVERE,
          String.format("Cannot write experiment settings to the file: %s.", fileName), e);
    } finally {
      if (ors != null) {
        try {
          ors.close();
        } catch (IOException e) {
          log.log(Level.WARNING,
              String.format("Cannot close output record stream: %s.", fileName), e);
        }
      }
    }
  }

  /** Deletes the file defined by the fileName. Failures in deleting the file
   * are logged and the exception is consumed w/in this method.
   *
   * @param fileName Name of the file to be deleted.
   */
  public void delete(String fileName) {
    try {
      final AbstractFile settingsFile = fileFactory.forFile(fileName);
      if (settingsFile.exists()) {
        settingsFile.delete();
      }
    } catch (IOException e) {
      log.log(Level.SEVERE,
          String.format("Cannot delete experiment settings file: %s.", fileName), e);
    }
  }
}
