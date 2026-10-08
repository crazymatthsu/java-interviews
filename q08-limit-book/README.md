# 08 — One-symbol price ladder

60 minutes. Best bid and best offer without a tree.

## Prompt

For a liquid cash equity the interesting prices sit in a band around the last sale. A `TreeMap` gives you log n and an allocator. A dense array gives you an index from the price.

Implement one symbol:

- `add(side, price, qty)` and `reduce(side, price, qty)` at aggregated price levels
- `bestBidPrice` / `bestAskPrice` in O(1) while the level stays non-empty
- `sweep(aggressor, qty)` walks from the touch and returns the residual
- prices outside `[low, low + tick * levels)` or off-tick are rejected, not inserted into a side structure

## Constraints

- No `TreeMap`, no `LinkedList` of orders, no per-resting-order object. This is a level book, not a full FIFO. Say that before you start: FIFO would be a second structure hanging off the level.
- Do not match inside `add`. A locked book is representable. Matching is `sweep`. The OMS owns that policy.
- Quantity math uses `addExact` or an explicit overflow check. A wrapped negative size is a bad print waiting to happen.

## What good looks like

- Best index updates in O(1) when the new price is better.
- When a level hits zero, rescan from the previous best toward the worse prices only.
- Sweep of a buy takes the lowest ask first. Partial level remains and stays the best ask.

## Follow-ups

- How do you hang FIFO orders off a level without allocating on cancel-by-id?
- When is a sparse hash of prices the better book (options strikes, a wide penny name)?
- What is wrong with rebuilding the best price by scanning the whole band on every add?
- Where does this sit relative to the venue book — a local child-order book, or a market-data aggregate?
