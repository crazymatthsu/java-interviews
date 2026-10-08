# 02 — Order ticket pool

45 minutes. The send path is not allowed to allocate.

## Prompt

A child order is a handful of primitives: client order id, symbol id, side, quantity, price in ticks. Building a new object per new/cancel/replace creates garbage the order thread will stop for.

Implement a fixed pool:

- `Ticket acquire()` — a recycled slot, or `null` if the pool is empty (the caller sheds; the pool does not grow)
- `boolean release(Ticket)` — returns the slot, clears business fields, rejects a double-free
- no allocation on acquire or release

## Constraints

- A ticket handed to another thread can be recycled out from under a stale holder. A generation (or ABA stamp) must make that visible. This solution uses an odd generation while live and an even generation while free.
- Do not use `ThreadLocal` pools unless you can say who returns the ticket and what happens when the IO thread releases a ticket the gateway allocated.
- `null` from `acquire` is backpressure, not an invitation to `new Ticket()`.

## What good looks like

- One array of tickets allocated in the constructor, plus an int stack of free indexes.
- `release` is idempotent-safe: the second call returns false and does not push the index twice.
- Business fields are wiped before the slot can be acquired again, so a recycled ticket cannot leak the previous client id into a new order.

## Follow-ups

- Why is a generation better than "I promise not to touch it after release"?
- Where would you size this pool for a session that can have 50k live child orders?
- What is wrong with `SynchronizedPool` or `ConcurrentLinkedQueue` of tickets on the send path?
- How does this interact with the SPSC ring in question 01 (ring of indexes, not of objects)?
