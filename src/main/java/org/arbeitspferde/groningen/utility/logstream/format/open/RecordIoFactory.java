package org.arbeitspferde.groningen.utility.logstream.format.open;

import com.matttproud.jawzall.format.recordio.Encoder;
import com.matttproud.jawzall.io.RecordIoOutputStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStreamFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link RecordIoFactory} furnishes {@link OutputLogStream}s that use use Sawzall's underlying
 * encoding for entries.
 *
 * {@inheritDoc}
 */
@ThreadSafe
public class RecordIoFactory implements OutputLogStreamFactory {
  private static final Encoder recordIoEncoder = new Encoder();

  @Override
  public OutputLogStream forStream(final OutputStream stream) throws IOException {
    return new RecordIo(new RecordIoOutputStream(stream, recordIoEncoder));
  }

  @Override
  public OutputLogStream rotatingStreamForSpecification(final Specification specification) throws IOException {
    final String baseName = String.format("%s.on_port_%s.log",
        specification.getFilenamePrefix(), specification.getServingPort());
    final FileOutputStream outputStream = new FileOutputStream(baseName);
    return new RecordIo(new RecordIoOutputStream(outputStream, recordIoEncoder));
  }
}
