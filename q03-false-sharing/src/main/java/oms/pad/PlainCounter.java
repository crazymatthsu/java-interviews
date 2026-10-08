package oms.pad;

/** Same contract as {@link Sequence} with no padding. Useful as the "before" in a bench. */
public final class PlainCounter {
    private volatile long value;

    public PlainCounter(long initial) {
        this.value = initial;
    }

    public long get() {
        return value;
    }

    public void set(long next) {
        value = next;
    }

    public void add(long delta) {
        value += delta;
    }
}
