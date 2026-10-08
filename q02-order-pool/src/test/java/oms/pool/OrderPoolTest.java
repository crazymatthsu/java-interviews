package oms.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderPoolTest {

    @Test
    void exhaustsThenRecyclesWithoutAllocatingANewTicket() {
        OrderPool pool = new OrderPool(2);
        OrderPool.Ticket first = pool.acquire();
        OrderPool.Ticket second = pool.acquire();
        assertEquals(0, pool.available());
        assertNull(pool.acquire());

        first.clOrdId = 42;
        first.qty = 100;
        int generation = first.generation();
        assertTrue(first.live());
        assertTrue(pool.release(first));
        assertFalse(first.live());
        assertEquals(0L, first.clOrdId);
        assertFalse(pool.release(first), "double free");

        OrderPool.Ticket again = pool.acquire();
        assertSame(first, again);
        assertTrue(again.live());
        assertTrue(again.generation() > generation);
        assertEquals(0L, again.qty);
        assertTrue(second.live());
        assertEquals(0, pool.available());
    }

    @Test
    void rejectsForeignTicket() {
        OrderPool pool = new OrderPool(1);
        OrderPool.Ticket foreign = new OrderPool(1).acquire();
        assertFalse(pool.release(foreign));
        assertFalse(pool.release(null));
    }
}
