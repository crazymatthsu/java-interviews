package oms.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TickMathTest {

    @Test
    void parsesCashEquitiesScale() {
        assertEquals(1_502_500L, TickMath.parseTicks("150.25"));
        assertEquals(1_502_500L, TickMath.parseTicks("150.2500"));
        assertEquals(100L, TickMath.parseTicks("0.01"));
        assertEquals(5L, TickMath.parseTicks("0.0005"));
        assertEquals(15_000_000L, TickMath.parseTicks("1500"));
        assertEquals("150.2500", TickMath.format(1_502_500L));
        assertEquals("0.0100", TickMath.format(100));
    }

    @Test
    void rejectsAFifthDecimalAndGarbage() {
        assertThrows(IllegalArgumentException.class, () -> TickMath.parseTicks("1.00001"));
        assertThrows(IllegalArgumentException.class, () -> TickMath.parseTicks("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> TickMath.parseTicks(""));
        assertThrows(IllegalArgumentException.class, () -> TickMath.parseTicks("12a"));
    }

    @Test
    void alignsToAPennyAndComputesNotional() {
        long penny = 100;
        long price = TickMath.parseTicks("150.2530");
        assertEquals(TickMath.parseTicks("150.25"), TickMath.align(price, penny, -1));
        assertEquals(TickMath.parseTicks("150.26"), TickMath.align(price, penny, 1));
        assertEquals(TickMath.parseTicks("150.25"), TickMath.align(price, penny, 0));
        assertEquals(price, TickMath.align(price, 1, 0));

        long notional = TickMath.notionalTicks(100, TickMath.parseTicks("150.25"));
        assertEquals(150_250_000L, notional);
        assertEquals(15_025L, notional / TickMath.SCALE);
        assertThrows(ArithmeticException.class, () -> TickMath.notionalTicks(Long.MAX_VALUE, 2));
    }
}
