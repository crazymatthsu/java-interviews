package oms.pool;

/**
 * Fixed pool of order tickets. Acquire and release do not allocate.
 * Generation is odd while the ticket is live and even while it is free,
 * so a double-release is rejected and a holder can see that a ticket was recycled.
 */
public final class OrderPool {
    private final Ticket[] tickets;
    private final int[] free;
    private int freeTop;

    public OrderPool(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        tickets = new Ticket[capacity];
        free = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            Ticket ticket = new Ticket();
            ticket.index = i;
            ticket.generation = 0;
            tickets[i] = ticket;
            free[i] = capacity - 1 - i;
        }
        freeTop = capacity;
    }

    public int capacity() {
        return tickets.length;
    }

    public int available() {
        return freeTop;
    }

    /** @return a live ticket, or null when the pool is exhausted */
    public Ticket acquire() {
        if (freeTop == 0) {
            return null;
        }
        int index = free[--freeTop];
        Ticket ticket = tickets[index];
        ticket.generation++;
        return ticket;
    }

    /** @return false on a double-free or a ticket that did not come from this pool */
    public boolean release(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        int index = ticket.index;
        if (index < 0 || index >= tickets.length || tickets[index] != ticket) {
            return false;
        }
        if ((ticket.generation & 1) == 0) {
            return false;
        }
        ticket.clOrdId = 0L;
        ticket.symbolId = 0;
        ticket.side = 0;
        ticket.qty = 0L;
        ticket.priceTicks = 0L;
        ticket.generation++;
        free[freeTop++] = index;
        return true;
    }

    public static final class Ticket {
        int index;
        int generation;
        public long clOrdId;
        public int symbolId;
        public byte side;
        public long qty;
        public long priceTicks;

        public int generation() {
            return generation;
        }

        public boolean live() {
            return (generation & 1) == 1;
        }
    }
}
