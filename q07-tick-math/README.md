# 07 — Prices are ticks, not BigDecimal

45 minutes. The edge parses once. The order thread never sees a decimal type.

## Prompt

US cash equities in this OMS are stored as a count of 0.0001 USD. A one-cent tick is 100 of those units. `BigDecimal` on the new-order path allocates, locks a math context, and makes overflow someone else's problem.

Implement, in longs:

- `parseTicks(String)` — at most 4 decimal places, reject a 5th, no regex
- `format(long)` — the inverse, for drop-copy and logs only
- `align(price, tickSize, mode)` — down, up, or nearest half-up, onto the instrument tick
- `notionalTicks(qty, price)` — `Math.multiplyExact`, never a silent wrap

## Constraints

- No `double`. `150.25` is not a double you then scale.
- No `BigDecimal`, no `String.format` on the align/notional path.
- Parsing may allocate the result of `format` only. `align` and `notionalTicks` allocate nothing and throw `ArithmeticException` on overflow rather than wrapping to a negative notional that passes a risk check.

## What good looks like

- `150.25` → `1_502_500`. One hundred shares → notional `150_250_000` ticks → `$15,025.00`.
- A price of `150.253` with a penny tick aligns down to `150.25` and up to `150.26`.
- Negative prices are rejected by `align`. A leading minus is accepted by `parse` only so you can talk about why a cash equity price should not be negative.

## Follow-ups

- Where does FX, or a futures tick of 0.25, break a hardcoded scale of 4?
- Why is half-up the wrong rounding mode for a passive peg, and which mode does the venue use?
- How do you test that a risk check uses the same notional formula as the drop copy?
