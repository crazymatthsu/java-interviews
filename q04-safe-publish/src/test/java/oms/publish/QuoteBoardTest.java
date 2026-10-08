package oms.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class QuoteBoardTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new QuoteBoard().publish(null));
    }

    @Test
    void readerNeverObservesATornQuote() throws Exception {
        QuoteBoard board = new QuoteBoard();
        int n = 100_000;
        AtomicBoolean torn = new AtomicBoolean();
        Thread reader = new Thread(() -> {
            long last = -1;
            while (last < n) {
                Quote quote = board.read();
                if (quote == null) {
                    Thread.onSpinWait();
                    continue;
                }
                if (quote.sequence() < last
                        || quote.symbolId() != 42
                        || quote.bidTicks() != quote.sequence()
                        || quote.askTicks() != quote.sequence() + 1
                        || quote.bidQty() != quote.sequence()
                        || quote.askQty() != 7) {
                    torn.set(true);
                    return;
                }
                last = quote.sequence();
            }
        });
        reader.start();
        for (long i = 1; i <= n; i++) {
            board.publish(new Quote(42, i, i + 1, i, 7, i));
        }
        reader.join(10_000);
        assertFalse(reader.isAlive(), "reader stuck");
        assertFalse(torn.get());
        assertEquals(n, board.read().sequence());
    }
}
