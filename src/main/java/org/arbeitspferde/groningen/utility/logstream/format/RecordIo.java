package org.arbeitspferde.groningen.utility.logstream.format;

import com.google.protobuf.Message;
import org.arbeitspferde.groningen.utility.logstream.OutputLogStream;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: mtp
 * Date: 10/20/12
 * Time: 13:33
 * To change this template use File | Settings | File Templates.
 */
@NotThreadSafe
public class RecordIo implements OutputLogStream {
    private static final int MAX_UNSIGNED_VARINT64_LENGTH = 10;

    private String encodeUnsignedVarint32(final String prefix, final long recordSize) {
        final StringBuilder builder = new StringBuilder();

        final long mask = 128;

        if (recordSize < (1 << 7)) {
            builder.append(recordSize);
        } else if (recordSize < (1<<14)) {
            builder.append(recordSize | mask);
            builder.append(recordSize >>7);
        } else if (recordSize < (1<<21)) {
            builder.append(recordSize | mask);
            builder.append((recordSize >> 7) | mask);
            builder.append(recordSize >> 14);
        } else if (recordSize < (1<<28)) {
            builder.append(recordSize | mask);
            builder.append((recordSize >> 7) | mask);
            builder.append((recordSize>>14) | mask);
            builder.append(recordSize>>21);
        } else {
            *(ptr++) = v | B;
            *(ptr++) = (v>>7) | B;
            *(ptr++) = (v>>14) | B;
            *(ptr++) = (v>>21) | B;
            *(ptr++) = v>>28;
        }
        return builder.toString();
    }

    private String encodeUnsignedVarint64(final String prefix, final long recordSize) {
        if (recordSize < (1 << 28)) {
            return encodeUnsignedVarint32(prefix, recordSize);
        }
        final StringBuilder builder = new StringBuilder();

        final int mask = 128;
        final long value32 = recordSize;

        builder.append(value32 | mask)
               .append((value32 >> 7) | mask)
               .append((value32 >> 14) | mask)
               .append((value32 >> 21) | mask);

        if (value32 < (1 << 35)) {
            builder.append(recordSize >> 28);
            return builder.toString();
        }

        builder.append((recordSize >> 28) | mask);

        return encodeUnsignedVarint32(builder.toString(), recordSize >> 35);
    }

    @Override
    public void write(final Message message) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void flush() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
