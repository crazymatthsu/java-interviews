package oms.wait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SequenceGateTest {

    @Test
    void returnsImmediatelyWhenAlreadyPublished() {
        SequenceGate gate = new SequenceGate();
        gate.publish(3);
        assertEquals(3L, gate.await(3, 1_000_000L));
        assertEquals(3L, gate.await(2, 1_000_000L));
    }

    @Test
    void timesOutWhenNothingIsPublished() {
        SequenceGate gate = new SequenceGate();
        long start = System.nanoTime();
        long got = gate.await(0, 2_000_000L);
        long elapsed = System.nanoTime() - start;
        assertEquals(-1L, got);
        assertTrue(elapsed < 500_000_000L, "timeout ran away: " + elapsed);
    }

    @Test
    void publishWakesAParkedWaiter() throws Exception {
        SequenceGate gate = new SequenceGate();
        long[] got = {-2L};
        Thread waiter = new Thread(() -> got[0] = gate.await(5, 2_000_000_000L));
        waiter.start();
        Thread.sleep(40);
        gate.publish(4);
        gate.publish(5);
        waiter.join(2_000);
        assertFalse(waiter.isAlive(), "waiter stuck");
        assertEquals(5L, got[0]);
    }
}
