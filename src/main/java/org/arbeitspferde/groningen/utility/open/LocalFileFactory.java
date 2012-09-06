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

package org.arbeitspferde.groningen.utility.open;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.annotation.concurrent.NotThreadSafe;

import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.FileFactory;

import com.google.inject.Singleton;

/**
 * Produce {@link AbstractFile}s that proxy {@link LocalFile} and
 * {@link AbstractFile} operations.
 */
@Singleton
public class LocalFileFactory implements FileFactory {
	private static final Logger log = Logger.getLogger(LocalFileFactory.class.getCanonicalName());

	@Override
	public AbstractFile forFile(String path, String mode) throws IOException {
		log.info(String.format("Creating LocalFile proxy for '%s' '%s'.", path, mode));
		return new LocalFileProxy(path, mode);
	}

	@Override
	public AbstractFile forFile(String path) throws IOException {
		log.info(String.format("Creating LocalFile proxy for '%s' 'r'", path));
		return new LocalFileProxy(path, "r");
	}

	@NotThreadSafe
	private class LocalFileProxy implements AbstractFile {
	  private final String path;
		private final String accessMode;

		public LocalFileProxy(final String path, final String accessMode) {
			this.path = path;
			this.accessMode = accessMode;
		}

		@Override
		public boolean delete() throws IOException, SecurityException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean exists() throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public OutputStream outputStreamFor() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InputStream inputStreamFor() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void renameTo(String newName) throws IOException {
			// TODO Auto-generated method stub

		}

	}

}
