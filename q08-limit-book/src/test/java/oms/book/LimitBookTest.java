package oms.book;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LimitBookTest {

    @Test
    void bestPricesTrackAddsAndReduces() {
        LimitBook book = ladder();
        assertEquals(-1L, book.bestBidPrice());
        assertEquals(-1L, book.bestAskPrice());
        assertEquals(LimitBook.OK, book.add(LimitBook.BUY, 100, 10));
        assertEquals(LimitBook.OK, book.add(LimitBook.BUY, 101, 5));
        assertEquals(LimitBook.OK, book.add(LimitBook.SELL, 103, 8));
        assertEquals(LimitBook.OK, book.add(LimitBook.SELL, 105, 4));
        assertEquals(101L, book.bestBidPrice());
        assertEquals(103L, book.bestAskPrice());

        assertEquals(LimitBook.OK, book.reduce(LimitBook.BUY, 101, 5));
        assertEquals(100L, book.bestBidPrice());
        assertEquals(LimitBook.BAD, book.reduce(LimitBook.BUY, 100, 11));
        assertEquals(LimitBook.OUT_OF_BAND, book.add(LimitBook.BUY, 50, 1));
        assertEquals(LimitBook.OUT_OF_BAND, book.add(LimitBook.SELL, 200, 1));
    }

    @Test
    void offTickAndSweep() {
        LimitBook book = new LimitBook(10_000, 100, 50);
        assertEquals(LimitBook.OUT_OF_BAND, book.add(LimitBook.BUY, 10_050, 1));
        assertEquals(LimitBook.OK, book.add(LimitBook.SELL, 10_200, 10));
        assertEquals(LimitBook.OK, book.add(LimitBook.SELL, 10_300, 10));
        assertEquals(LimitBook.OK, book.add(LimitBook.SELL, 10_400, 10));
        assertEquals(0L, book.sweep(LimitBook.BUY, 25));
        assertEquals(0L, book.quantityOnAsk(10_200));
        assertEquals(0L, book.quantityOnAsk(10_300));
        assertEquals(5L, book.quantityOnAsk(10_400));
        assertEquals(10_400L, book.bestAskPrice());
        assertEquals(0L, book.sweep(LimitBook.BUY, 5));
        assertEquals(-1L, book.bestAskPrice());
        assertEquals(3L, book.sweep(LimitBook.BUY, 3));
    }

    @Test
    void sellSweepHitsTheBestBidFirst() {
        LimitBook book = ladder();
        book.add(LimitBook.BUY, 100, 4);
        book.add(LimitBook.BUY, 102, 4);
        assertEquals(0L, book.sweep(LimitBook.SELL, 6));
        assertEquals(0L, book.quantityOnBid(102));
        assertEquals(2L, book.quantityOnBid(100));
        assertEquals(100L, book.bestBidPrice());
    }

    private static LimitBook ladder() {
        return new LimitBook(100, 1, 20);
    }
}
