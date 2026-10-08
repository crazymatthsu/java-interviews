package oms.pad;

/**
 * Single-writer cursor. Padding keeps {@code value} off the cache line of a
 * neighbouring sequence that another core is writing (producer head vs consumer tail).
 * Padding does not make {@code ++} atomic across writers; this type has one writer.
 */
public final class Sequence {
    @SuppressWarnings("unused")
    private long p0, p1, p2, p3, p4, p5, p6;
    private volatile long value;
    @SuppressWarnings("unused")
    private long s0, s1, s2, s3, s4, s5, s6;

    public Sequence(long initial) {
        this.value = initial;
        this.p0 = 1L;
        this.s0 = 1L;
    }

    public long get() {
        return value;
    }

    public void set(long next) {
        value = next;
    }

    /** Single writer only. A volatile long write is atomic; the read-modify-write is not. */
    public void add(long delta) {
        value += delta;
    }
}
