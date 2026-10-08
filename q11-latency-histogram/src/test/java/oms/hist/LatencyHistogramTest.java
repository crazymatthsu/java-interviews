package oms.hist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LatencyHistogramTest {

    @Test
    void percentileLandsOnTheBucketEdge() {
        LatencyHistogram histogram = new LatencyHistogram();
        assertEquals(0L, histogram.percentileNanos(99));
        for (int i = 0; i < 100; i++) {
            histogram.record(5_000L);
        }
        assertEquals(100L, histogram.count());
        assertEquals(12, LatencyHistogram.bucket(5_000L));
        assertEquals(4_096L, histogram.percentileNanos(50));
        assertEquals(4_096L, histogram.percentileNanos(100));
        histogram.record(50_000_000L);
        assertEquals(4_096L, histogram.percentileNanos(50));
        assertTrueBucket(histogram.percentileNanos(100), 50_000_000L);
        histogram.reset();
        assertEquals(0L, histogram.count());
        assertEquals(0L, histogram.percentileNanos(99));
    }

    @Test
    void rejectsAPercentilePastTheEnds() {
        assertThrows(IllegalArgumentException.class, () -> new LatencyHistogram().percentileNanos(101));
        assertThrows(IllegalArgumentException.class, () -> new LatencyHistogram().percentileNanos(-1));
    }

    private static void assertTrueBucket(long edge, long sample) {
        assertEquals(LatencyHistogram.low(LatencyHistogram.bucket(sample)), edge);
    }
}
