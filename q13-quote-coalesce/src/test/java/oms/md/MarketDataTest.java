package oms.md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarketDataTest {

    @Test
    void quotesCollapseToTheLatestPerSymbol() {
        NbboCoalescer book = new NbboCoalescer(4);
        assertTrue(book.onQuote(1, 10, 11, 1, 1, 1));
        assertTrue(book.onQuote(1, 12, 13, 2, 2, 2));
        assertFalse(book.onQuote(1, 1, 2, 9, 9, 0));
        assertTrue(book.onQuote(2, 20, 21, 3, 3, 5));
        assertEquals(2, book.pending());

        int[] ids = new int[4];
        long[] bid = new long[4];
        long[] ask = new long[4];
        long[] bsz = new long[4];
        long[] asz = new long[4];
        long[] seq = new long[4];
        assertEquals(2, book.drain(ids, bid, ask, bsz, asz, seq));
        assertEquals(1, ids[0]);
        assertEquals(12L, bid[0]);
        assertEquals(13L, ask[0]);
        assertEquals(2L, seq[0]);
        assertEquals(2, ids[1]);
        assertEquals(5L, seq[1]);
        assertEquals(0, book.drain(ids, bid, ask, bsz, asz, seq));
    }

    @Test
    void tradesAreKeptInOrderUntilTheTapeIsFull() {
        TradeTape tape = new TradeTape(2);
        assertTrue(tape.onTrade(1, 10, 100, 1));
        assertTrue(tape.onTrade(1, 11, 50, 2));
        assertFalse(tape.onTrade(1, 12, 50, 3));
        int[] ids = new int[2];
        long[] px = new long[2];
        long[] qty = new long[2];
        long[] seq = new long[2];
        assertEquals(2, tape.drain(ids, px, qty, seq));
        assertEquals(10L, px[0]);
        assertEquals(11L, px[1]);
        assertEquals(1L, seq[0]);
        assertEquals(2L, seq[1]);
        assertTrue(tape.onTrade(3, 7, 1, 9));
        assertEquals(1, tape.drain(ids, px, qty, seq));
        assertEquals(7L, px[0]);
        assertEquals(9L, seq[0]);
    }
}
