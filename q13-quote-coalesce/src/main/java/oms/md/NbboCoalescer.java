package oms.md;

/**
 * Last-value NBBO per symbol. Two quotes for the same id between drains
 * occupy one dirty slot; the later sequence wins. A strictly older sequence
 * is ignored. Trades do not belong here — see {@link TradeTape}.
 */
public final class NbboCoalescer {
    private final long[] bid;
    private final long[] ask;
    private final long[] bidSize;
    private final long[] askSize;
    private final long[] sequence;
    private final boolean[] seen;
    private final int[] dirty;
    private final boolean[] marked;
    private int dirtyCount;

    public NbboCoalescer(int symbols) {
        if (symbols <= 0) {
            throw new IllegalArgumentException("symbols");
        }
        bid = new long[symbols];
        ask = new long[symbols];
        bidSize = new long[symbols];
        askSize = new long[symbols];
        sequence = new long[symbols];
        seen = new boolean[symbols];
        dirty = new int[symbols];
        marked = new boolean[symbols];
    }

    public boolean onQuote(int symbolId, long bidPx, long askPx, long bSz, long aSz, long seq) {
        if (symbolId < 0 || symbolId >= bid.length || seq < 0) {
            return false;
        }
        if (seen[symbolId] && seq < sequence[symbolId]) {
            return false;
        }
        seen[symbolId] = true;
        bid[symbolId] = bidPx;
        ask[symbolId] = askPx;
        bidSize[symbolId] = bSz;
        askSize[symbolId] = aSz;
        sequence[symbolId] = seq;
        if (!marked[symbolId]) {
            marked[symbolId] = true;
            dirty[dirtyCount++] = symbolId;
        }
        return true;
    }

    public int drain(int[] symbolOut, long[] bidOut, long[] askOut,
                     long[] bidSizeOut, long[] askSizeOut, long[] seqOut) {
        int n = dirtyCount;
        if (symbolOut.length < n || bidOut.length < n || askOut.length < n
                || bidSizeOut.length < n || askSizeOut.length < n || seqOut.length < n) {
            throw new IllegalArgumentException("out");
        }
        for (int i = 0; i < n; i++) {
            int id = dirty[i];
            marked[id] = false;
            symbolOut[i] = id;
            bidOut[i] = bid[id];
            askOut[i] = ask[id];
            bidSizeOut[i] = bidSize[id];
            askSizeOut[i] = askSize[id];
            seqOut[i] = sequence[id];
        }
        dirtyCount = 0;
        return n;
    }

    public int pending() {
        return dirtyCount;
    }
}
