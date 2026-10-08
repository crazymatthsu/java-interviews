package oms.batch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OutboundBatcherTest {

    @Test
    void partialWritesPreserveOrderAndBackpressure() {
        OutboundBatcher batcher = new OutboundBatcher(8);
        assertTrue(batcher.tryAppend(new byte[] {1, 2, 3, 4}, 0, 4));
        assertTrue(batcher.tryAppend(new byte[] {5, 6, 7}, 0, 3));
        assertFalse(batcher.tryAppend(new byte[] {8, 9}, 0, 2));
        assertEquals(1, batcher.free());

        byte[] got = new byte[8];
        int[] at = {0};
        OutboundBatcher.ByteSink sink = (src, off, len) -> {
            int n = Math.min(3, len);
            System.arraycopy(src, off, got, at[0], n);
            at[0] += n;
            return n;
        };
        assertEquals(3, batcher.flush(sink));
        assertEquals(4, batcher.size());
        assertEquals(3, batcher.flush(sink));
        assertEquals(1, batcher.size());
        assertEquals(1, batcher.flush(sink));
        assertEquals(0, batcher.size());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}, Arrays.copyOf(got, 8));
        assertTrue(batcher.tryAppend(new byte[] {8, 9}, 0, 2));
        assertEquals(0, batcher.flush((src, off, len) -> 0));
        assertEquals(2, batcher.size());
    }
}
