package oms.tick;

/**
 * Prices are a count of 0.0001 USD (one tick of the internal scale).
 * A listed penny increment is 100 of those ticks. Notional is
 * {@code qty * priceTicks} in the same scale, via {@link Math#multiplyExact}.
 * Parsing is an edge concern. The order thread only sees longs.
 */
public final class TickMath {
    public static final int SCALE = 10_000;

    private TickMath() {}

    public static long parseTicks(String text) {
        if (text == null || text.isEmpty() || text.length() > 24) {
            throw new IllegalArgumentException("price");
        }
        int i = 0;
        boolean negative = false;
        if (text.charAt(0) == '-') {
            negative = true;
            i++;
        }
        if (i >= text.length()) {
            throw new IllegalArgumentException("price");
        }
        long whole = 0;
        int fracDigits = -1;
        long frac = 0;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.') {
                if (fracDigits >= 0) {
                    throw new IllegalArgumentException("price");
                }
                fracDigits = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("price");
            }
            int digit = c - '0';
            if (fracDigits < 0) {
                whole = Math.addExact(Math.multiplyExact(whole, 10L), digit);
            } else {
                if (fracDigits == 4) {
                    throw new IllegalArgumentException("scale");
                }
                frac = frac * 10L + digit;
                fracDigits++;
            }
        }
        if (fracDigits < 0) {
            fracDigits = 0;
        }
        for (int pad = fracDigits; pad < 4; pad++) {
            frac *= 10L;
        }
        long ticks = Math.addExact(Math.multiplyExact(whole, SCALE), frac);
        return negative ? Math.negateExact(ticks) : ticks;
    }

    /** Edge formatting. One {@code StringBuilder}, not a {@code BigDecimal}. */
    public static String format(long ticks) {
        boolean negative = ticks < 0;
        long value = negative ? Math.negateExact(ticks) : ticks;
        long whole = value / SCALE;
        long frac = value % SCALE;
        StringBuilder out = new StringBuilder(16);
        if (negative) {
            out.append('-');
        }
        out.append(whole).append('.');
        out.append((char) ('0' + (frac / 1000) % 10));
        out.append((char) ('0' + (frac / 100) % 10));
        out.append((char) ('0' + (frac / 10) % 10));
        out.append((char) ('0' + frac % 10));
        return out.toString();
    }

    /**
     * @param mode {@code -1} down, {@code 0} nearest half-up, {@code +1} up
     */
    public static long align(long priceTicks, long tickSize, int mode) {
        if (tickSize <= 0 || priceTicks < 0) {
            throw new IllegalArgumentException("align");
        }
        long remainder = priceTicks % tickSize;
        if (remainder == 0) {
            return priceTicks;
        }
        if (mode < 0) {
            return priceTicks - remainder;
        }
        if (mode > 0) {
            return Math.addExact(priceTicks - remainder, tickSize);
        }
        if (remainder * 2 >= tickSize) {
            return Math.addExact(priceTicks - remainder, tickSize);
        }
        return priceTicks - remainder;
    }

    public static long notionalTicks(long qty, long priceTicks) {
        if (qty < 0 || priceTicks < 0) {
            throw new IllegalArgumentException("sign");
        }
        return Math.multiplyExact(qty, priceTicks);
    }
}
