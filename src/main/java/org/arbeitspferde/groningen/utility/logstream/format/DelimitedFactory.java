package org.arbeitspferde.groningen.utility.logstream.format;

import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mtp
 * Date: 10/20/12
 * Time: 19:48
 * To change this template use File | Settings | File Templates.
 */
@ThreadSafe
public class DelimitedFactory implements OutputLogStreamFactory {
    @Override
    public OutputLogStream forStream(final OutputStream stream) throws IOException {
        return new Delimited(stream);
    }

    @Override
    public OutputLogStream rotatingStreamForSpecification(final Specification specification) throws IOException {
        final String baseName = String.format("%s.on_port_%s.log", specification.getFilenamePrefix(), specification.getServingPort());
        final FileOutputStream outputStream = new FileOutputStream(baseName);
        return new Delimited(outputStream);
    }
}
