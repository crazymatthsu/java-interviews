# 04 — Publishing an NBBO without a lock

30–45 minutes. Memory ordering, not another queue.

## Prompt

The market-data thread builds a quote for a symbol and the order thread must read bid, ask, sizes and a sequence as one snapshot. You do not need every intermediate quote. You do need to never observe a bid from update N and an ask from update N+1.

Implement a one-writer board:

- `void publish(Quote)`
- `Quote read()`
- `Quote` is immutable
- publication uses `VarHandle.setRelease` / `getAcquire` (a volatile field is an acceptable answer if you can say why)

## Constraints

- Do not lock. Do not copy fields under synchronization on the read path.
- A mutable quote object written field-by-field and then published by an unsynchronized reference store is wrong. Be ready to show the tear.
- `setRelease` on the reference does not make the *next* mutation of that same object safe. Immutability is part of the protocol, not a style preference.

## What good looks like

- The writer finishes every field (the constructor does this) before the release store.
- Readers spin or return null when nothing has been published yet; they never block the market-data thread.
- Sequence is monotonic from the reader's point of view. You will not see sequence 10 after you have seen 11 from this board, because each publish replaces the reference and the reader only moves forward.

## Follow-ups

- When is a volatile reference not enough (the quote is a flyweight over a `ByteBuffer` the writer reuses)?
- What does `setOpaque` give you that is too weak here?
- How would you publish a *batch* of symbols without the order thread allocating an array?
- Why is `AtomicReference` acceptable but not magically better than a `VarHandle`?
