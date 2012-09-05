/**
 * 
 */
package org.arbeitspferde.groningen.utility.open;

import java.io.IOException;

import org.arbeitspferde.groningen.utility.AbstractFile;
import org.arbeitspferde.groningen.utility.FileFactory;

/**
 * Produce {@link AbstractFile}s that proxy {@link LocalFile} and
 * {@link AbstractFile} operations.
 */
public class LocalFileFactory implements FileFactory {

	@Override
	public AbstractFile forFile(String path, String mode) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractFile forFile(String path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
