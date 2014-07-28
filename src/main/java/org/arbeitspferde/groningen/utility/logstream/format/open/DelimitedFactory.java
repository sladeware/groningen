package org.arbeitspferde.groningen.utility.logstream.format.open;

import com.google.protobuf.Message;

import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;

import javax.annotation.concurrent.ThreadSafe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>{@link DelimitedFactory} furnishes {@link OutputLogStream}s that use use Protocol Buffers'
 * underlying delimited encoding per {@link Message#writeDelimitedTo(OutputStream)}.
 * </p>
 *
 * {@inheritDoc}
 */
@ThreadSafe
public class DelimitedFactory implements OutputLogStreamFactory {
  @Override
  public OutputLogStream forStream(final OutputStream stream) throws IOException {
    return new Delimited(stream);
  }

  @Override
  public OutputLogStream rotatingStreamForSpecification(final Specification specification)
      throws IOException {
    final String baseName = String.format("%s.on_port_%s.log",
        specification.getFilenamePrefix(), specification.getServingPort());
    final FileOutputStream outputStream = new FileOutputStream(baseName);

    return new Delimited(outputStream);
  }
}
