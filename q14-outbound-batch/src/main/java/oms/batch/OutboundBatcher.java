package oms.batch;

/**
 * Stages encoded frames for one socket. {@link #tryAppend} fails when the
 * staging buffer cannot hold the frame — the producer sheds or retries.
 * {@link #flush} hands bytes to a sink that may accept a prefix only.
 * The unwritten tail is compacted to the front. No allocation on either call.
 */
public final class OutboundBatcher {
    private final byte[] data;
    private int size;

    public OutboundBatcher(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.data = new byte[capacity];
    }

    public int capacity() {
        return data.length;
    }

    public int size() {
        return size;
    }

    public int free() {
        return data.length - size;
    }

    public boolean tryAppend(byte[] frame, int off, int len) {
        if (frame == null || len < 0 || off < 0 || off > frame.length - len) {
            throw new IllegalArgumentException("frame");
        }
        if (len == 0) {
            return true;
        }
        if (size > data.length - len) {
            return false;
        }
        System.arraycopy(frame, off, data, size, len);
        size += len;
        return true;
    }

    /** @return bytes the sink accepted */
    public int flush(ByteSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink");
        }
        if (size == 0) {
            return 0;
        }
        int wrote = sink.write(data, 0, size);
        if (wrote < 0 || wrote > size) {
            throw new IllegalStateException("sink wrote " + wrote);
        }
        if (wrote == 0) {
            return 0;
        }
        int rest = size - wrote;
        if (rest > 0) {
            System.arraycopy(data, wrote, data, 0, rest);
        }
        size = rest;
        return wrote;
    }

    @FunctionalInterface
    public interface ByteSink {
        int write(byte[] src, int off, int len);
    }
}
