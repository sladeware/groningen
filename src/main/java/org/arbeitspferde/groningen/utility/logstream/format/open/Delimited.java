package org.arbeitspferde.groningen.utility.logstream.format.open;

import com.google.protobuf.Message;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStream;

@NotThreadSafe
class Delimited implements OutputLogStream {
  private final OutputStream proxied;

  Delimited(final OutputStream proxied) {
    this.proxied = proxied;
  }

  @Override
  public void write(final Message message) throws IOException {
    message.writeDelimitedTo(proxied);
  }

  @Override
  public void close() throws IOException {
    proxied.close();
  }

  @Override
  public void flush() throws IOException {
    proxied.flush();
  }
}