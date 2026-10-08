package oms.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChildOrderTest {

    @Test
    void fillsThroughPartialToDone() {
        ChildOrder order = new ChildOrder(100);
        assertFalse(order.onFill(1));
        assertTrue(order.onAck());
        assertFalse(order.onAck());
        assertTrue(order.onFill(40));
        assertEquals(ChildOrder.State.PARTIALLY_FILLED, order.state());
        assertEquals(60L, order.leavesQty());
        assertFalse(order.onFill(61));
        assertTrue(order.onFill(60));
        assertEquals(ChildOrder.State.FILLED, order.state());
        assertEquals(0L, order.leavesQty());
        assertFalse(order.onCancelRequest());
        assertConserved(order);
    }

    @Test
    void cancelLeavesAPartialFillIntact() {
        ChildOrder order = new ChildOrder(100);
        assertTrue(order.onAck());
        assertTrue(order.onFill(25));
        assertTrue(order.onCancelRequest());
        assertEquals(ChildOrder.State.PENDING_CANCEL, order.state());
        assertTrue(order.onFill(10));
        assertEquals(ChildOrder.State.PENDING_CANCEL, order.state());
        assertEquals(35L, order.cumQty());
        assertTrue(order.onCancelAck());
        assertEquals(ChildOrder.State.CANCELED, order.state());
        assertEquals(65L, order.canceledQty());
        assertEquals(0L, order.leavesQty());
        assertFalse(order.onFill(1));
        assertConserved(order);
    }

    @Test
    void fillDuringCancelCanFinishTheOrder() {
        ChildOrder order = new ChildOrder(10);
        order.onAck();
        order.onCancelRequest();
        assertTrue(order.onFill(10));
        assertEquals(ChildOrder.State.FILLED, order.state());
        assertFalse(order.onCancelAck());
        assertConserved(order);
    }

    @Test
    void rejectIsOnlyFromPendingNew() {
        ChildOrder order = new ChildOrder(10);
        assertTrue(order.onReject());
        assertEquals(ChildOrder.State.REJECTED, order.state());
        assertEquals(10L, order.canceledQty());
        assertEquals(0L, order.leavesQty());
        assertFalse(order.onAck());
        assertConserved(order);
    }

    private static void assertConserved(ChildOrder order) {
        assertEquals(order.orderQty(), order.cumQty() + order.canceledQty() + order.leavesQty());
    }
}
