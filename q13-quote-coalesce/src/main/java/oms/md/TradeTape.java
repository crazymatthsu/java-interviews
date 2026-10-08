package oms.md;

/**
 * Every print is kept, in order, until the consumer drains.
 * Returning false when the tape is full is intentional: dropping a trade
 * is a different bug from coalescing a quote.
 */
public final class TradeTape {
    private final int[] symbol;
    private final long[] price;
    private final long[] qty;
    private final long[] sequence;
    private final int capacity;
    private int write;
    private int size;

    public TradeTape(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
        symbol = new int[capacity];
        price = new long[capacity];
        qty = new long[capacity];
        sequence = new long[capacity];
    }

    public boolean onTrade(int symbolId, long px, long quantity, long seq) {
        if (size == capacity || quantity <= 0) {
            return false;
        }
        symbol[write] = symbolId;
        price[write] = px;
        qty[write] = quantity;
        sequence[write] = seq;
        write++;
        if (write == capacity) {
            write = 0;
        }
        size++;
        return true;
    }

    public int drain(int[] symbolOut, long[] priceOut, long[] qtyOut, long[] seqOut) {
        int n = size;
        if (symbolOut.length < n || priceOut.length < n || qtyOut.length < n || seqOut.length < n) {
            throw new IllegalArgumentException("out");
        }
        int start = write - size;
        if (start < 0) {
            start += capacity;
        }
        for (int k = 0; k < n; k++) {
            int i = start + k;
            if (i >= capacity) {
                i -= capacity;
            }
            symbolOut[k] = symbol[i];
            priceOut[k] = price[i];
            qtyOut[k] = qty[i];
            seqOut[k] = sequence[i];
        }
        size = 0;
        write = 0;
        return n;
    }

    public int size() {
        return size;
    }
}
