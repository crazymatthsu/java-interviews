package oms.fix;

/**
 * Outbound FIX sequence and a bounded memory of what we sent.
 * {@link #planResend} answers a ResendRequest. It does not move {@link #nextSeq()}:
 * live traffic keeps its own high-water while replay is a separate cursor.
 *
 * <p>Consecutive admin messages collapse into one gap fill. Application
 * messages are resent one by one. If {@code begin} is older than the buffer,
 * the plan returns {@link #TOO_OLD} rather than inventing fills we no longer have.
 */
public final class FixOutbound {
    public static final int ADMIN = 1;
    public static final int APP = 2;

    public static final int EMPTY = 0;
    public static final int TOO_OLD = -1;
    public static final int BAD_RANGE = -2;

    private final long[] seqs;
    private final byte[] kinds;
    private final int mask;
    private final int capacity;
    private long oldest = -1L;
    private long nextSeq = 1L;

    public FixOutbound(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two and >= 2");
        }
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.seqs = new long[capacity];
        this.kinds = new byte[capacity];
    }

    public long nextSeq() {
        return nextSeq;
    }

    public long send(int kind) {
        if (kind != ADMIN && kind != APP) {
            throw new IllegalArgumentException("kind");
        }
        long seq = nextSeq++;
        int index = (int) (seq & mask);
        seqs[index] = seq;
        kinds[index] = (byte) kind;
        if (oldest < 0) {
            oldest = seq;
        }
        long retained = nextSeq - oldest;
        if (retained > capacity) {
            oldest = nextSeq - capacity;
        }
        return seq;
    }

    /**
     * @return number of actions written, {@link #EMPTY}, {@link #TOO_OLD}, or {@link #BAD_RANGE}
     */
    public int planResend(long begin, long end, Action[] out) {
        if (out == null || begin <= 0 || end < begin) {
            return BAD_RANGE;
        }
        long lastSent = nextSeq - 1L;
        if (lastSent < 1L || begin > lastSent) {
            return EMPTY;
        }
        if (end > lastSent) {
            end = lastSent;
        }
        if (begin < oldest) {
            return TOO_OLD;
        }
        int n = 0;
        long i = begin;
        while (i <= end) {
            if (n >= out.length) {
                throw new IllegalArgumentException("action buffer");
            }
            if (kindAt(i) == ADMIN) {
                long start = i;
                i++;
                while (i <= end && kindAt(i) == ADMIN) {
                    i++;
                }
                Action action = out[n++];
                action.type = Action.GAP_FILL;
                action.beginSeq = start;
                action.newSeqNo = i;
                action.msgSeq = 0L;
            } else {
                Action action = out[n++];
                action.type = Action.RESEND;
                action.beginSeq = i;
                action.msgSeq = i;
                action.newSeqNo = 0L;
                i++;
            }
        }
        return n;
    }

    private int kindAt(long seq) {
        int index = (int) (seq & mask);
        if (seqs[index] != seq) {
            throw new IllegalStateException("seq " + seq + " is not in the resend buffer");
        }
        return kinds[index];
    }

    public static final class Action {
        public static final int RESEND = 1;
        public static final int GAP_FILL = 2;

        public int type;
        public long beginSeq;
        public long newSeqNo;
        public long msgSeq;
    }
}
