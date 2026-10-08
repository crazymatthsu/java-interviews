package oms.book;

/**
 * One symbol, dense ladder. Index zero is {@code lowPrice}, each step is {@code tick}.
 * Best bid is the highest non-empty bid index. Best ask is the lowest non-empty ask index.
 * Adding is O(1). A level going empty rescans from the previous best, which is cheap
 * inside a band and is the thing you would replace with a bitmap if the band were wide.
 *
 * <p>The ladder does not match on add. Crossing is allowed until somebody calls {@link #sweep}.
 * That split is deliberate: the OMS decides whether this book is a view or a matching engine.
 */
public final class LimitBook {
    public static final byte BUY = 1;
    public static final byte SELL = 2;

    public static final int OK = 0;
    public static final int BAD = 1;
    public static final int OUT_OF_BAND = 2;

    private final long lowPrice;
    private final long tick;
    private final int levels;
    private final long[] bidQty;
    private final long[] askQty;
    private int bestBid = -1;
    private int bestAsk = -1;

    public LimitBook(long lowPrice, long tick, int levels) {
        if (tick <= 0 || levels <= 0 || lowPrice < 0) {
            throw new IllegalArgumentException("ladder");
        }
        this.lowPrice = lowPrice;
        this.tick = tick;
        this.levels = levels;
        this.bidQty = new long[levels];
        this.askQty = new long[levels];
    }

    public int add(byte side, long price, long qty) {
        int index = index(price);
        if (qty <= 0 || (side != BUY && side != SELL)) {
            return BAD;
        }
        if (index < 0) {
            return OUT_OF_BAND;
        }
        if (side == BUY) {
            bidQty[index] = Math.addExact(bidQty[index], qty);
            if (bestBid < 0 || index > bestBid) {
                bestBid = index;
            }
        } else {
            askQty[index] = Math.addExact(askQty[index], qty);
            if (bestAsk < 0 || index < bestAsk) {
                bestAsk = index;
            }
        }
        return OK;
    }

    public int reduce(byte side, long price, long qty) {
        int index = index(price);
        if (qty <= 0 || (side != BUY && side != SELL)) {
            return BAD;
        }
        if (index < 0) {
            return OUT_OF_BAND;
        }
        long[] book = side == BUY ? bidQty : askQty;
        if (book[index] < qty) {
            return BAD;
        }
        book[index] -= qty;
        if (book[index] == 0) {
            retarget(side);
        }
        return OK;
    }

    /** @return quantity that did not trade */
    public long sweep(byte aggressor, long qty) {
        if (qty <= 0) {
            return qty;
        }
        if (aggressor == BUY) {
            while (qty > 0 && bestAsk >= 0) {
                long take = Math.min(askQty[bestAsk], qty);
                askQty[bestAsk] -= take;
                qty -= take;
                if (askQty[bestAsk] == 0) {
                    retarget(SELL);
                }
            }
            return qty;
        }
        if (aggressor == SELL) {
            while (qty > 0 && bestBid >= 0) {
                long take = Math.min(bidQty[bestBid], qty);
                bidQty[bestBid] -= take;
                qty -= take;
                if (bidQty[bestBid] == 0) {
                    retarget(BUY);
                }
            }
            return qty;
        }
        throw new IllegalArgumentException("side");
    }

    public long bestBidPrice() {
        return bestBid < 0 ? -1L : priceOf(bestBid);
    }

    public long bestAskPrice() {
        return bestAsk < 0 ? -1L : priceOf(bestAsk);
    }

    public long quantityOnBid(long price) {
        int index = index(price);
        return index < 0 ? 0L : bidQty[index];
    }

    public long quantityOnAsk(long price) {
        int index = index(price);
        return index < 0 ? 0L : askQty[index];
    }

    private void retarget(byte side) {
        if (side == BUY) {
            for (int i = Math.max(bestBid, 0); i >= 0; i--) {
                if (bidQty[i] != 0) {
                    bestBid = i;
                    return;
                }
            }
            bestBid = -1;
            return;
        }
        int start = bestAsk < 0 ? 0 : bestAsk;
        for (int i = start; i < levels; i++) {
            if (askQty[i] != 0) {
                bestAsk = i;
                return;
            }
        }
        bestAsk = -1;
    }

    private int index(long price) {
        if (price < lowPrice) {
            return -1;
        }
        long delta = price - lowPrice;
        if (delta % tick != 0) {
            return -1;
        }
        long index = delta / tick;
        if (index >= levels) {
            return -1;
        }
        return (int) index;
    }

    private long priceOf(int index) {
        return lowPrice + (long) index * tick;
    }
}
