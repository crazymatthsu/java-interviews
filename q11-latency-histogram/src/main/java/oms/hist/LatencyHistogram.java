package oms.hist;

/**
 * Log-sized latency buckets, one count per power of two.
 * Bucket {@code k >= 1} covers {@code [2^k, 2^(k+1))} nanoseconds.
 * Bucket 0 covers anything below 2ns. {@link #percentileNanos} returns the
 * inclusive lower edge of the bucket that contains the percentile.
 * Recording does not allocate and does not take a lock. One writer.
 */
public final class LatencyHistogram {
    public static final int BUCKETS = 64;
    private final long[] counts = new long[BUCKETS];
    private long total;

    public void record(long nanos) {
        counts[bucket(nanos)]++;
        total++;
    }

    public long count() {
        return total;
    }

    public void reset() {
        for (int i = 0; i < BUCKETS; i++) {
            counts[i] = 0L;
        }
        total = 0L;
    }

    /** @param percentile inclusive, 0 through 100 */
    public long percentileNanos(int percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("percentile");
        }
        if (total == 0) {
            return 0L;
        }
        long target = (long) Math.ceil((percentile / 100.0) * total);
        if (target < 1) {
            target = 1;
        }
        long seen = 0;
        for (int i = 0; i < BUCKETS; i++) {
            seen += counts[i];
            if (seen >= target) {
                return low(i);
            }
        }
        return low(BUCKETS - 1);
    }

    public static int bucket(long nanos) {
        if (nanos <= 1L) {
            return 0;
        }
        int log = 63 - Long.numberOfLeadingZeros(nanos);
        return Math.min(log, BUCKETS - 1);
    }

    public static long low(int bucket) {
        if (bucket <= 0) {
            return 0L;
        }
        return 1L << bucket;
    }
}
