package oms.fsm;

/**
 * Sell-side child order, single writer. Illegal transitions return false
 * and change nothing. Fills are allowed while a cancel is pending; a full
 * fill then wins over the cancel. Terminal states are filled, canceled, rejected.
 */
public final class ChildOrder {
    public enum State {
        PENDING_NEW,
        NEW,
        PARTIALLY_FILLED,
        PENDING_CANCEL,
        FILLED,
        CANCELED,
        REJECTED
    }

    private final long orderQty;
    private long cumQty;
    private long canceledQty;
    private State state = State.PENDING_NEW;

    public ChildOrder(long orderQty) {
        if (orderQty <= 0) {
            throw new IllegalArgumentException("qty");
        }
        this.orderQty = orderQty;
    }

    public State state() {
        return state;
    }

    public long orderQty() {
        return orderQty;
    }

    public long cumQty() {
        return cumQty;
    }

    public long canceledQty() {
        return canceledQty;
    }

    public long leavesQty() {
        return orderQty - cumQty - canceledQty;
    }

    public boolean onAck() {
        if (state != State.PENDING_NEW) {
            return false;
        }
        state = State.NEW;
        return true;
    }

    public boolean onReject() {
        if (state != State.PENDING_NEW) {
            return false;
        }
        canceledQty = orderQty;
        state = State.REJECTED;
        return true;
    }

    public boolean onFill(long qty) {
        if (qty <= 0 || qty > leavesQty()) {
            return false;
        }
        if (state != State.NEW && state != State.PARTIALLY_FILLED && state != State.PENDING_CANCEL) {
            return false;
        }
        cumQty += qty;
        if (leavesQty() == 0) {
            state = State.FILLED;
        } else if (state == State.NEW) {
            state = State.PARTIALLY_FILLED;
        }
        return true;
    }

    public boolean onCancelRequest() {
        if (state != State.NEW && state != State.PARTIALLY_FILLED) {
            return false;
        }
        state = State.PENDING_CANCEL;
        return true;
    }

    public boolean onCancelAck() {
        if (state != State.PENDING_CANCEL) {
            return false;
        }
        canceledQty += leavesQty();
        state = State.CANCELED;
        return true;
    }
}
