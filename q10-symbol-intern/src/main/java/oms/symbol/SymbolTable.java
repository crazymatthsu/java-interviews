package oms.symbol;

/**
 * Open-addressed interner from raw ticker bytes to a dense id starting at 1.
 * Lookup compares bytes and allocates nothing. Intern copies the slice once,
 * the first time the symbol is seen. Symbols are not removed during the session.
 * Length is capped at 16 bytes (ticker plus a suffix).
 */
public final class SymbolTable {
    private final byte[][] bytes;
    private final int[] lengths;
    private final int[] ids;
    private final byte[] state;
    private final int mask;
    private int size;
    private int nextId = 1;

    public SymbolTable(int capacity) {
        int cap = 4;
        while (cap < Math.max(capacity, 4)) {
            cap <<= 1;
        }
        bytes = new byte[cap][];
        lengths = new int[cap];
        ids = new int[cap];
        state = new byte[cap];
        mask = cap - 1;
    }

    public int intern(byte[] raw, int off, int len) {
        if (!valid(raw, off, len) || len > 16) {
            throw new IllegalArgumentException("symbol");
        }
        int found = find(raw, off, len);
        if (found >= 0) {
            return ids[found];
        }
        if (size * 2 >= state.length) {
            throw new IllegalStateException("full");
        }
        int dest = empty(raw, off, len);
        byte[] copy = new byte[len];
        System.arraycopy(raw, off, copy, 0, len);
        bytes[dest] = copy;
        lengths[dest] = len;
        int id = nextId++;
        ids[dest] = id;
        state[dest] = 1;
        size++;
        return id;
    }

    /** @return id, or {@code -1} when the symbol is unknown */
    public int lookup(byte[] raw, int off, int len) {
        if (!valid(raw, off, len)) {
            return -1;
        }
        int found = find(raw, off, len);
        return found >= 0 ? ids[found] : -1;
    }

    private int find(byte[] raw, int off, int len) {
        int i = mix(raw, off, len);
        int start = i;
        for (;;) {
            if (state[i] == 0) {
                return -1;
            }
            if (lengths[i] == len && equals(bytes[i], raw, off, len)) {
                return i;
            }
            i = (i + 1) & mask;
            if (i == start) {
                return -1;
            }
        }
    }

    private int empty(byte[] raw, int off, int len) {
        int i = mix(raw, off, len);
        int start = i;
        for (;;) {
            if (state[i] == 0) {
                return i;
            }
            i = (i + 1) & mask;
            if (i == start) {
                throw new IllegalStateException("full");
            }
        }
    }

    private static boolean valid(byte[] raw, int off, int len) {
        return raw != null && len > 0 && off >= 0 && off <= raw.length - len;
    }

    private static boolean equals(byte[] stored, byte[] raw, int off, int len) {
        for (int i = 0; i < len; i++) {
            if (stored[i] != raw[off + i]) {
                return false;
            }
        }
        return true;
    }

    private int mix(byte[] raw, int off, int len) {
        int hash = 0x811C9DC5;
        for (int i = 0; i < len; i++) {
            hash ^= raw[off + i] & 0xFF;
            hash *= 0x01000193;
        }
        return hash & mask;
    }
}
