# 10 — Ticker bytes to a dense id

45 minutes. The order path does not carry a `String`.

## Prompt

Symbols arrive as bytes (from a binary frame, or from the FIX tag parser at the edge). After that they are `int` ids. Implement an open-addressed table:

- `int intern(byte[] raw, int off, int len)` — existing id or a new dense id starting at 1
- `int lookup(byte[] raw, int off, int len)` — id, or `-1`
- lookup allocates nothing and does not insert
- intern copies the slice once, the first time only
- maximum length 16 bytes
- no remove (the session does not forget `BRK.B` at lunch)

## Constraints

- No `HashMap<String, Integer>`, no `String` constructed just to look up, no `ByteBuffer` decode to a `String`.
- Case-sensitive. `AAPL` and `aapl` are different symbols until a *different* component folds case. Do not hide that here.
- Power-of-two table, linear probe, load factor at most a half. Failing closed when full is better than rehashing on the order thread. Say so.

## What good looks like

- Hash the slice (FNV-1a is fine). Probe compares length and then bytes.
- The same bytes at a different offset in a larger array return the same id.
- Ids are dense and stable for the process lifetime, so they can index the risk gates and the coalescer.

## Follow-ups

- How do you pre-load the universe before the open so the first order does not intern?
- What is the suffix policy for `BRK.B` versus `BRK/B` versus a CMS symbol, and why does it not belong inside `equals` as a silent rewrite?
- When would you rehash, and on which thread?
