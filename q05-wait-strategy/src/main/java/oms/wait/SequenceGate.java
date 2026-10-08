package oms.wait;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 * Idle wait for a single sequencer. Hot path spins, then yields, then parks.
 * The producer unparks the waiter after the release store. The consumer
 * re-reads the sequence after storing its thread into {@code waiter}, so a
 * publish that lands in between cannot be missed.
 *
 * <p>Sequences are non-negative. {@code -1} is reserved for timeout.
 * Virtual threads are the wrong tool here: the order thread must stay on a core.
 */
public final class SequenceGate {
    private static final VarHandle SEQ;
    private static final VarHandle WAITER;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            SEQ = lookup.findVarHandle(SequenceGate.class, "sequence", long.class);
            WAITER = lookup.findVarHandle(SequenceGate.class, "waiter", Thread.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private long sequence = -1L;
    @SuppressWarnings("unused")
    private Thread waiter;

    public void publish(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("sequence");
        }
        SEQ.setRelease(this, value);
        Thread parked = (Thread) WAITER.getAcquire(this);
        if (parked != null) {
            LockSupport.unpark(parked);
        }
    }

    public long sequence() {
        return (long) SEQ.getAcquire(this);
    }

    /**
     * @return the observed sequence, which is {@code >= target}, or {@code -1} on timeout
     */
    public long await(long target, long timeoutNanos) {
        if (target < 0 || timeoutNanos < 0) {
            throw new IllegalArgumentException("await");
        }
        long start = System.nanoTime();
        int spins = 0;
        while (true) {
            long observed = (long) SEQ.getAcquire(this);
            if (observed >= target) {
                return observed;
            }
            if (System.nanoTime() - start >= timeoutNanos) {
                return -1L;
            }
            if (spins < 64) {
                spins++;
                Thread.onSpinWait();
            } else if (spins < 256) {
                spins++;
                Thread.yield();
            } else {
                WAITER.setRelease(this, Thread.currentThread());
                observed = (long) SEQ.getAcquire(this);
                if (observed >= target) {
                    WAITER.setRelease(this, null);
                    return observed;
                }
                long remaining = timeoutNanos - (System.nanoTime() - start);
                if (remaining <= 0) {
                    WAITER.setRelease(this, null);
                    return -1L;
                }
                LockSupport.parkNanos(this, remaining);
                WAITER.setRelease(this, null);
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return -1L;
                }
            }
        }
    }
}
