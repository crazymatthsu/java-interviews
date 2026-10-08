# 09 — Inline pre-trade risk

60 minutes. The order does not leave the process until this returns ok.

## Prompt

Before a child order is sequenced onto the wire, reject it for a reason the desk can explain. One symbol, one account, no allocation, no exception for an ordinary reject.

Checks, in this order, returning on the first failure:

1. quantity is positive and at most `maxQty`, side is buy or sell
2. notional `qty * priceTicks` is at most `maxNotionalTicks`, and does not overflow
3. if `lastTicks > 0`, absolute distance from last is at most `collarTicks`
4. buy does not take `position + openBuy + qty` through `maxLong`; sell does not take short exposure through `maxShort`
5. client order id has not already been admitted

On success, reserve the working quantity and remember the id. `onFill` moves quantity from working into position. `onCancel` releases working quantity. `forget` drops the id so the client may reuse it after a cancel.

## Constraints

- A failed check must not reserve quantity and must not consume the id.
- Duplicate detection is an open-addressed set of longs, not a `HashSet<Long>`. Zero is a legal client id. Tombstones if you support `forget`.
- Do not throw `PreTradeException` on the hot path. A reason code is the API. Reasons: `QTY`, `NOTIONAL`, `COLLAR`, `POSITION`, `DUPLICATE`, `FULL`.

## What good looks like

- Cheapest predicates first. The id set is the most expensive successful check, so it runs last.
- Working quantity counts. Two live buys of 60 against a long limit of 100: the second is a position reject, not a fill-time surprise.
- Overflow of the notional multiply is a notional reject, not a wrapped negative that compares as "small".

## Follow-ups

- This gate is single-threaded. What breaks if the drop-copy thread calls `onFill` while the order thread calls `admit`?
- How do you extend one symbol to a dense array of gates keyed by symbol id?
- Where does locate / short-sale exempt belong, and why is it not a float?
- What is the difference between a hard reject and a hold for a human on a sell-side desk?
