# 06 — 32-byte child-order frame

45 minutes. The wire format inside the process, before FIX or OUCH.

## Prompt

Between the OMS core and the session handler, a child order is a fixed binary frame, not a `HashMap` of tags and not a JSON object. Implement encode and decode over a `ByteBuffer`:

- exactly 32 bytes, big-endian, fixed offsets (see the class comment)
- absolute gets and puts — do not move `position` or `limit`, other code owns the cursor
- caller-supplied `OrderFrame` flyweight, so decode does not allocate
- return `-1` when the slice is too small or the flyweight is null; do not throw on the hot path

## Constraints

- No `ByteArrayOutputStream`, no `DataOutputStream`, no intermediate `byte[]` per order.
- No `String` for symbol, side or TIF. Symbol is an `int` from the interner (question 10). Side and TIF are bytes.
- State the endianness out loud. The matching host and the gateway must agree, and little-endian x86 still does not make a network frame little-endian.

## What good looks like

- One flyweight reused by the sender. Fields are overwritten, not rebuilt.
- Two frames can sit back-to-back in one buffer.
- A short tail returns `-1` without writing a partial header. (Absolute multi-field writes can tear if you write some fields and then notice. Check capacity first.)

## Follow-ups

- Where would you switch this for SBE, and what does SBE still force you to understand?
- Why is a direct buffer not automatically faster than a heap buffer for a 32-byte frame that never hits the NIC?
- How do you version the frame when you add a desk id without moving every offset?
