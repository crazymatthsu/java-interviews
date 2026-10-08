package oms.risk;

/**
 * Inline pre-trade gate for one symbol account. Checks run cheapest first
 * and change no state until the order is admitted. Admission reserves
 * working quantity and records the client order id.
 *
 * <p>Codes: 0 ok, 1 qty, 2 notional, 3 collar, 4 position, 5 duplicate, 6 full.
 * Collar is skipped when {@code lastTicks == 0} (no print yet).
 */
public final class PreTradeGate {
    public static final int OK = 0;
    public static final int QTY = 1;
    public static final int NOTIONAL = 2;
    public static final int COLLAR = 3;
    public static final int POSITION = 4;
    public static final int DUPLICATE = 5;
    public static final int FULL = 6;

    public static final byte BUY = 1;
    public static final byte SELL = 2;

    private final long maxQty;
    private final long maxNotionalTicks;
    private final long collarTicks;
    private final long maxLong;
    private final long maxShort;
    private long position;
    private long openBuy;
    private long openSell;
    private final LongSet ids;

    public PreTradeGate(long maxQty, long maxNotionalTicks, long collarTicks,
                         long maxLong, long maxShort, int idSlots) {
        if (maxQty <= 0 || maxNotionalTicks <= 0 || collarTicks < 0 || maxLong < 0 || maxShort < 0) {
            throw new IllegalArgumentException("limits");
        }
        this.maxQty = maxQty;
        this.maxNotionalTicks = maxNotionalTicks;
        this.collarTicks = collarTicks;
        this.maxLong = maxLong;
        this.maxShort = maxShort;
        this.ids = new LongSet(idSlots);
    }

    public int admit(long clOrdId, byte side, long qty, long priceTicks, long lastTicks) {
        if (qty <= 0 || qty > maxQty || (side != BUY && side != SELL)) {
            return QTY;
        }
        if (priceTicks < 0) {
            return NOTIONAL;
        }
        long notion;
        try {
            notion = Math.multiplyExact(qty, priceTicks);
        } catch (ArithmeticException ex) {
            return NOTIONAL;
        }
        if (notion > maxNotionalTicks) {
            return NOTIONAL;
        }
        if (lastTicks > 0) {
            long distance = priceTicks > lastTicks ? priceTicks - lastTicks : lastTicks - priceTicks;
            if (distance > collarTicks) {
                return COLLAR;
            }
        }
        if (side == BUY) {
            if (position + openBuy + qty > maxLong) {
                return POSITION;
            }
        } else if (-position + openSell + qty > maxShort) {
            return POSITION;
        }
        int put = ids.add(clOrdId);
        if (put == 0) {
            return DUPLICATE;
        }
        if (put < 0) {
            return FULL;
        }
        if (side == BUY) {
            openBuy += qty;
        } else {
            openSell += qty;
        }
        return OK;
    }

    public void onFill(byte side, long qty) {
        if (qty <= 0) {
            return;
        }
        if (side == BUY) {
            openBuy = Math.max(0L, openBuy - qty);
            position += qty;
        } else if (side == SELL) {
            openSell = Math.max(0L, openSell - qty);
            position -= qty;
        }
    }

    public void onCancel(byte side, long qty) {
        if (qty <= 0) {
            return;
        }
        if (side == BUY) {
            openBuy = Math.max(0L, openBuy - qty);
        } else if (side == SELL) {
            openSell = Math.max(0L, openSell - qty);
        }
    }

    public boolean forget(long clOrdId) {
        return ids.remove(clOrdId);
    }

    public long position() {
        return position;
    }

    public long openBuy() {
        return openBuy;
    }

    public long openSell() {
        return openSell;
    }

    /** Open-addressed set of client order ids. 0 is a legal key. No allocation after construction. */
    static final class LongSet {
        private final long[] keys;
        private final byte[] state;
        private final int mask;
        private int size;

        LongSet(int capacity) {
            int cap = 4;
            while (cap < capacity) {
                cap <<= 1;
            }
            keys = new long[cap];
            state = new byte[cap];
            mask = cap - 1;
        }

        /** @return 1 inserted, 0 duplicate, -1 full */
        int add(long key) {
            int i = mix(key);
            int start = i;
            int tomb = -1;
            for (;;) {
                byte slot = state[i];
                if (slot == 1 && keys[i] == key) {
                    return 0;
                }
                if (slot == 2 && tomb < 0) {
                    tomb = i;
                }
                if (slot == 0) {
                    break;
                }
                i = (i + 1) & mask;
                if (i == start) {
                    break;
                }
            }
            if (size * 2 >= keys.length) {
                return -1;
            }
            int dest = tomb >= 0 ? tomb : i;
            if (state[dest] != 0 && state[dest] != 2) {
                return -1;
            }
            state[dest] = 1;
            keys[dest] = key;
            size++;
            return 1;
        }

        boolean remove(long key) {
            int i = mix(key);
            int start = i;
            for (;;) {
                byte slot = state[i];
                if (slot == 0) {
                    return false;
                }
                if (slot == 1 && keys[i] == key) {
                    state[i] = 2;
                    size--;
                    return true;
                }
                i = (i + 1) & mask;
                if (i == start) {
                    return false;
                }
            }
        }

        private int mix(long key) {
            long z = key * 0x9E3779B97F4A7C15L;
            return (int) (z >>> 32) & mask;
        }
    }
}
