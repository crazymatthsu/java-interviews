package oms.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreTradeGateTest {

    private PreTradeGate gate() {
        return new PreTradeGate(1_000, 1_000_000, 50, 100, 50, 16);
    }

    @Test
    void rejectsQtyNotionalAndCollarWithoutReserving() {
        PreTradeGate gate = gate();
        assertEquals(PreTradeGate.QTY, gate.admit(1, PreTradeGate.BUY, 0, 10, 10));
        assertEquals(PreTradeGate.QTY, gate.admit(1, PreTradeGate.BUY, 1_001, 10, 10));
        assertEquals(PreTradeGate.NOTIONAL, gate.admit(1, PreTradeGate.BUY, 100, 20_000, 20_000));
        assertEquals(PreTradeGate.COLLAR, gate.admit(1, PreTradeGate.BUY, 1, 200, 10));
        assertEquals(0L, gate.openBuy());
        assertEquals(PreTradeGate.OK, gate.admit(1, PreTradeGate.BUY, 1, 10, 0));
    }

    @Test
    void positionCountsWorkingQuantityAndFills() {
        PreTradeGate gate = gate();
        assertEquals(PreTradeGate.OK, gate.admit(7, PreTradeGate.BUY, 60, 10, 10));
        assertEquals(PreTradeGate.POSITION, gate.admit(8, PreTradeGate.BUY, 50, 10, 10));
        assertEquals(60L, gate.openBuy());
        gate.onFill(PreTradeGate.BUY, 60);
        assertEquals(0L, gate.openBuy());
        assertEquals(60L, gate.position());
        assertEquals(PreTradeGate.OK, gate.admit(8, PreTradeGate.BUY, 40, 10, 10));
        gate.onCancel(PreTradeGate.BUY, 40);
        assertEquals(0L, gate.openBuy());
    }

    @Test
    void shortLimitAndDuplicateClientId() {
        PreTradeGate gate = gate();
        assertEquals(PreTradeGate.OK, gate.admit(3, PreTradeGate.SELL, 40, 10, 10));
        assertEquals(PreTradeGate.POSITION, gate.admit(4, PreTradeGate.SELL, 20, 10, 10));
        assertEquals(PreTradeGate.DUPLICATE, gate.admit(3, PreTradeGate.SELL, 1, 10, 10));
        assertTrue(gate.forget(3));
        assertFalse(gate.forget(3));
        gate.onCancel(PreTradeGate.SELL, 40);
        assertEquals(PreTradeGate.OK, gate.admit(3, PreTradeGate.SELL, 10, 10, 10));
        gate.onFill(PreTradeGate.SELL, 10);
        assertEquals(-10L, gate.position());
    }
}
