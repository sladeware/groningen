package org.arbeitspferde.groningen.utility.logstream.format;

import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mtp
 * Date: 10/20/12
 * Time: 13:36
 * To change this template use File | Settings | File Templates.
 */
@ThreadSafe
public class RecordIoFactory implements OutputLogStreamFactory {
    @Override
    public OutputLogStream forStream(final OutputStream stream) throws IOException {
        return null;
    }

    @Override
    public OutputLogStream rotatingStreamForSpecification(final Specification specification) throws IOException {
        return null;
    }
}
