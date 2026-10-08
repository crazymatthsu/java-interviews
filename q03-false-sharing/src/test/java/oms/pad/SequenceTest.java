package oms.pad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class SequenceTest {

    @Test
    void singleWriterAddsExactly() {
        Sequence sequence = new Sequence(10);
        sequence.add(5);
        sequence.set(3);
        assertEquals(3L, sequence.get());
    }

    @Test
    void neighboursDoNotLoseUpdates() throws Exception {
        int n = 250_000;
        long[] padded = OwnershipBench.padded(n);
        long[] plain = OwnershipBench.plain(n);
        assertEquals(n, padded[0]);
        assertEquals(n, padded[1]);
        assertEquals(n, plain[0]);
        assertEquals(n, plain[1]);
        assertTrue(padded[2] > 0);
        assertTrue(plain[2] > 0);
    }

    @Test
    void paddedSequenceCarriesACacheLineOfLongs() {
        int longs = 0;
        for (Field field : Sequence.class.getDeclaredFields()) {
            if (field.getType() == long.class && !Modifier.isStatic(field.getModifiers())) {
                longs++;
            }
        }
        assertTrue(longs >= 15, "expected value plus padding longs, got " + longs);
    }
}
