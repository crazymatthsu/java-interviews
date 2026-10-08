package oms.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SymbolTableTest {

    @Test
    void internsOnceAndIsCaseSensitive() {
        SymbolTable table = new SymbolTable(32);
        byte[] aapl = "AAPL".getBytes(StandardCharsets.US_ASCII);
        byte[] again = "xxAAPLyy".getBytes(StandardCharsets.US_ASCII);
        byte[] lower = "aapl".getBytes(StandardCharsets.US_ASCII);
        int id = table.intern(aapl, 0, aapl.length);
        assertEquals(id, table.intern(again, 2, 4));
        assertEquals(id, table.lookup(aapl, 0, 4));
        assertEquals(-1, table.lookup(lower, 0, 4));
        int other = table.intern(lower, 0, 4);
        assertEquals(id + 1, other);
        assertEquals(-1, table.lookup("MSFT".getBytes(StandardCharsets.US_ASCII), 0, 3));
    }

    @Test
    void rejectsEmptyAndOverlong() {
        SymbolTable table = new SymbolTable(8);
        byte[] bytes = "ABCDEFGHIJKLMNOPQ".getBytes(StandardCharsets.US_ASCII);
        assertThrows(IllegalArgumentException.class, () -> table.intern(bytes, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> table.intern(bytes, 0, 17));
        assertEquals(-1, table.lookup(bytes, 0, 0));
    }
}
