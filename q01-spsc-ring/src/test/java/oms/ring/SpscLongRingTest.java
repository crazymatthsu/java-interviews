package oms.ring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpscLongRingTest {

    @Test
    void rejectsNonPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> new SpscLongRing(3));
        assertThrows(IllegalArgumentException.class, () -> new SpscLongRing(1));
    }

    @Test
    void fullEmptyAndWrap() {
        SpscLongRing ring = new SpscLongRing(4);
        SpscLongRing.Slot slot = new SpscLongRing.Slot();
        assertFalse(ring.poll(slot));
        assertTrue(ring.offer(10));
        assertTrue(ring.offer(0));
        assertTrue(ring.offer(-3));
        assertTrue(ring.offer(40));
        assertFalse(ring.offer(50));

        assertTrue(ring.poll(slot));
        assertEquals(10L, slot.value);
        assertTrue(ring.offer(50));

        assertTrue(ring.poll(slot));
        assertEquals(0L, slot.value);
        assertTrue(ring.poll(slot));
        assertEquals(-3L, slot.value);
        assertTrue(ring.poll(slot));
        assertEquals(40L, slot.value);
        assertTrue(ring.poll(slot));
        assertEquals(50L, slot.value);
        assertFalse(ring.poll(slot));
    }

    @Test
    void concurrentOrderIsPreserved() throws Exception {
        int n = 200_000;
        SpscLongRing ring = new SpscLongRing(1024);
        int[] errors = {0};
        Thread consumer = new Thread(() -> {
            SpscLongRing.Slot slot = new SpscLongRing.Slot();
            long expect = 1;
            int got = 0;
            while (got < n) {
                if (!ring.poll(slot)) {
                    Thread.onSpinWait();
                    continue;
                }
                if (slot.value != expect) {
                    errors[0]++;
                    return;
                }
                expect++;
                got++;
            }
        });
        consumer.start();
        for (long i = 1; i <= n; i++) {
            while (!ring.offer(i)) {
                Thread.onSpinWait();
            }
        }
        consumer.join(10_000);
        assertFalse(consumer.isAlive(), "consumer stuck");
        assertEquals(0, errors[0]);
    }
}
