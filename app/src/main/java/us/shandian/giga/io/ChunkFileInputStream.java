package us.shandian.giga.io;

import org.schabi.newpipe.streams.io.SharpStream;

import java.io.IOException;

public class ChunkFileInputStream extends SharpStream {
    private static final int REPORT_INTERVAL = 256 * 1024;

    private SharpStream source;
    private final long offset;
    private final long length;
    private long position;

    private long progressReport;
    private final ProgressReport onProgress;

    public ChunkFileInputStream(SharpStream target, long start, long end, ProgressReport callback) throws IOException {
        source = target;
        offset = start;
        length = end - start;
        position = 0;
        onProgress = callback;
        progressReport = REPORT_INTERVAL;

        if (length < 1) {
            source.close();
            throw new IOException("The chunk is empty or invalid");
        }
        if (source.length() < end) {
            try {
                throw new IOException(String.format("invalid file length. expected = %s  found = %s", end, source.length()));
            } finally {
                source.close();
            }
        }

        source.seek(offset);
    }

    /**
     * Get absolute position on file
     *
     * @return the position
     */
    public long getFilePointer() {
        return offset + position;
    }

    @Override
    public int read() throws IOException {
        if (position >= length) {
            return -1; // EOF
        }

        int res = source.read();
        if (res >= 0) {
            position++;
        }
        return res;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position >= length) {
            return -1; // EOF
        }

        if ((position + len) > length) {
            len = (int) (length - position);
        }

        int res = source.read(b, off, len);
        if (res > 0) {
            position += res;

            if (onProgress != null && position > progressReport) {
                onProgress.report(position);
                progressReport = position + REPORT_INTERVAL;
            }
        }

        return res;
    }

    @Override
    public long skip(long pos) throws IOException {
        pos = Math.min(pos + position, length);

        if (pos == 0) {
            return 0;
        }

        source.seek(offset + pos);

        long oldPos = position;
        position = pos;

        return pos - oldPos;
    }

    @Override
    public long available() {
        // If position exceeds length, no bytes are available
        if (position > length) {
            return 0;
        }
        return Math.min(Long.MAX_VALUE, length - position);
    }

    @SuppressWarnings("EmptyCatchBlock")
    @Override
    public void close() {
        source.close();
        source = null;
    }

    @Override
    public boolean isClosed() {
        return source == null;
    }

    @Override
    public void rewind() throws IOException {
        position = 0;
        source.seek(offset);
    }

    @Override
    public boolean canRewind() {
        return true;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public void write(byte value) {
    }

    @Override
    public void write(byte[] buffer) {
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
    }

}
