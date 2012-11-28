package org.arbeitspferde.groningen.utility.logstream.format;

import com.google.protobuf.Message;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStream;

@NotThreadSafe
public class Delimited implements OutputLogStream {
    private final OutputStream outputStream;

    public Delimited(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(final Message message) throws IOException {
        message.writeDelimitedTo(outputStream);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }
}
