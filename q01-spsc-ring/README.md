# 01 — SPSC ring of order ids

45–60 minutes. Gateway thread to the single risk thread.

## Prompt

The order gateway receives a child-order id (`long`) and must hand it to the risk thread in order. Either side can run ahead of the other for a short stretch.

Implement a single-producer, single-consumer queue of primitive longs:

- `boolean offer(long)` — `false` when the ring is full
- `boolean poll(Slot)` — `false` when empty; otherwise writes the value into the caller-supplied slot
- power-of-two capacity, fixed at construction
- no locks, no `synchronized`, no `java.util.concurrent` queues
- no allocation after the constructor returns

## Constraints

- Do not box the long. A zero id is a legal value, so emptiness cannot be a sentinel.
- Full and empty must be distinguishable. Leaving one slot unused is acceptable; this solution uses explicit sequences and treats `tail - head == capacity` as full, so every slot is usable.
- The consumer must never observe a new tail together with a stale slot. Say which store is the release and which load is the acquire.

## What good looks like

- The producer caches the last head it observed; the consumer caches the last tail. A hot empty or full check then does not bounce a cache line on every call.
- Head and tail live in separately allocated, cache-line-padded objects. One writer each.
- Plain store into the array, then a volatile store of `tail`. Volatile load of `tail`, then a plain load of the slot.

## Follow-ups

- The ring carries only a long. Where does the `Order` live, and who allocates it?
- What breaks if a second risk thread calls `poll`?
- Why is `ArrayBlockingQueue` the wrong default on this path?
- How does this differ from the LMAX Disruptor (`MultiProducerSequencer`, gating sequences, `WaitStrategy`)?
