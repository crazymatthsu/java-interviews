# 12 — Child-order state machine

45–60 minutes. One writer. The quantities have to add up.

## Prompt

A sell-side child order starts at `PENDING_NEW`. Implement the transitions the venue can force on you:

- ack → `NEW`
- reject, only from `PENDING_NEW` → `REJECTED`
- fill → `PARTIALLY_FILLED` or `FILLED`
- cancel request from `NEW` or `PARTIALLY_FILLED` → `PENDING_CANCEL`
- cancel ack → `CANCELED`
- a fill is still legal in `PENDING_CANCEL`. If it consumes the leaves, the order is `FILLED` and the cancel ack must fail

`cumQty + canceledQty + leavesQty == orderQty` after every successful call. A rejected call changes nothing.

## Constraints

- No locks. The order thread is the only writer. Document that; do not add `synchronized` "to be safe".
- Do not use a boolean soup (`acked`, `canceled`, `filled`) that can be true together. One enum.
- Overfill is rejected. A zero or negative fill is rejected. You do not throw for those; you return false. A throw is for a broken caller (negative order quantity at construction).

## What good looks like

- Terminal states (`FILLED`, `CANCELED`, `REJECTED`) have zero leaves.
- Reject sets `canceledQty` to the original quantity so the identity still holds and risk can release the reservation.
- Cancel ack cancels only what is left. It does not erase `cumQty`.

## Follow-ups

- Where does `PENDING_REPLACE` go, and what is the quantity risk between the replace request and the ack?
- A busted fill arrives after `FILLED`. What state do you add, and why is it not a silent edit of `cumQty`?
- How does this object stay single-writer if the FIX thread and the risk thread both want it?
