# 13 — Coalesce quotes, keep every print

45 minutes. Market-data fan-out into the OMS.

## Prompt

The strategy and the peg logic need the latest NBBO, not every intermediate quote. They need every trade. Those are different pipes.

Implement both, with caller-supplied output arrays (no allocation on drain):

- `NbboCoalescer` — `onQuote` keeps the latest sequence per symbol id and a dirty set. Two updates to the same symbol between drains drain once. A strictly older sequence is ignored.
- `TradeTape` — `onTrade` appends. It returns false when the tape is full. It does not overwrite the oldest print.

## Constraints

- Symbol id is a dense int from the interner, used as an array index. Do not key a `HashMap`.
- Do not drop a trade to "keep up". Backpressure (`false`) is the signal to the IO thread.
- Quote coalesce is last-value. Say the sentence out loud: quotes coalesce, trades do not. A candidate who coalesces both has built a wrong OMS.

## What good looks like

- Dirty flag per symbol so a hot name does not appear twice in one drain.
- Stale quote does not clear a newer pending quote.
- Trade drain preserves order across the ring wrap. After a full drain the tape accepts again.

## Follow-ups

- What is the latency cost of draining every symbol versus draining a dirty list?
- How do you detect a locked or crossed NBBO, and do you still publish it?
- Where does a sequence gap on the *trade* feed become a halt in trading, rather than a coalesce?
