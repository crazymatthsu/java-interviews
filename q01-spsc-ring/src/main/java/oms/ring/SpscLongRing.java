package oms.ring;

/**
 * Single-producer, single-consumer ring of primitive longs.
 * One thread calls {@link #offer}; a different thread calls {@link #poll}.
 * No locks and no allocation after construction.
 *
 * <p>The producer writes the slot, then release-stores {@code tail}.
 * The consumer acquire-loads {@code tail} before it reads the slot.
 * Head is the mirror image, owned by the consumer.
 * {@code tail - head == capacity} means full, so every slot is usable.
 */
public final class SpscLongRing {
    private final long[] data;
    private final int mask;
    private final int capacity;

    /** Producer-owned cursor. Separate object so it does not share a line with head. */
    private final PaddedLong tail = new PaddedLong();
    /** Consumer-owned cursor. */
    private final PaddedLong head = new PaddedLong();

    /** Producer-only cache of the consumer cursor. */
    private long cachedHead;
    /** Consumer-only cache of the producer cursor. */
    private long cachedTail;

    public SpscLongRing(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two and >= 2");
        }
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.data = new long[capacity];
    }

    public int capacity() {
        return capacity;
    }

    public boolean offer(long value) {
        long t = tail.value;
        if (t - cachedHead >= capacity) {
            cachedHead = head.value;
            if (t - cachedHead >= capacity) {
                return false;
            }
        }
        data[(int) (t & mask)] = value;
        tail.value = t + 1L;
        return true;
    }

    public boolean poll(Slot slot) {
        long h = head.value;
        if (h == cachedTail) {
            cachedTail = tail.value;
            if (h == cachedTail) {
                return false;
            }
        }
        slot.value = data[(int) (h & mask)];
        head.value = h + 1L;
        return true;
    }

    public static final class Slot {
        public long value;
    }

    /** 7 longs on each side of the cursor. Two instances cannot share the line that holds value. */
    static final class PaddedLong {
        @SuppressWarnings("unused")
        long p0, p1, p2, p3, p4, p5, p6;
        volatile long value;
        @SuppressWarnings("unused")
        long s0, s1, s2, s3, s4, s5, s6;

        PaddedLong() {
            p0 = s0 = 1L;
        }
    }
}
