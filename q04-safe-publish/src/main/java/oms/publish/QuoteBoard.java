package oms.publish;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * One writer publishes a whole quote. Any number of readers take a snapshot.
 * {@code setRelease} / {@code getAcquire} are the explicit form of a volatile
 * reference: the writer stores every field of the quote before the release,
 * and a reader who sees the reference sees those fields.
 *
 * <p>That is only true because {@link Quote} is immutable. A mutable quote
 * published this way tears.
 */
public final class QuoteBoard {
    private static final VarHandle SNAPSHOT;

    static {
        try {
            SNAPSHOT = MethodHandles.lookup().findVarHandle(QuoteBoard.class, "snapshot", Quote.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private Quote snapshot;

    public void publish(Quote next) {
        if (next == null) {
            throw new IllegalArgumentException("quote");
        }
        SNAPSHOT.setRelease(this, next);
    }

    public Quote read() {
        return (Quote) SNAPSHOT.getAcquire(this);
    }
}
