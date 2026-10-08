package oms.pad;

/**
 * Two threads, two cursors, each thread writes only its own cursor.
 * Lost updates would be a bug. False sharing only shows up as extra time,
 * and only when the two cursors sit on one cache line.
 */
public final class OwnershipBench {
    private OwnershipBench() {}

    public static long[] padded(int iterations) throws InterruptedException {
        Sequence left = new Sequence(0);
        Sequence right = new Sequence(0);
        long nanos = hammer(iterations, left::add, right::add);
        return new long[] {left.get(), right.get(), nanos};
    }

    public static long[] plain(int iterations) throws InterruptedException {
        PlainCounter left = new PlainCounter(0);
        PlainCounter right = new PlainCounter(0);
        long nanos = hammer(iterations, left::add, right::add);
        return new long[] {left.get(), right.get(), nanos};
    }

    private static long hammer(int iterations, java.util.function.LongConsumer left,
                               java.util.function.LongConsumer right) throws InterruptedException {
        Thread a = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                left.accept(1L);
            }
        });
        Thread b = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                right.accept(1L);
            }
        });
        long start = System.nanoTime();
        a.start();
        b.start();
        a.join();
        b.join();
        return System.nanoTime() - start;
    }
}
