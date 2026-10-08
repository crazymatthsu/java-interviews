# 15 — FIX resend without stalling the live sequence

60–70 minutes. The counterparty gaps. The desk does not stop sending.

## Prompt

You are the outbound side of a FIX session. Each send takes the next sequence and is either admin (heartbeat, test request, sequence reset) or application (new, cancel, replace, execution). The counterparty sends `ResendRequest(begin, end)`.

Implement:

- `long send(int kind)` — allocates the next sequence and remembers the kind in a power-of-two ring
- `int planResend(long begin, long end, Action[] out)` — fills a caller-supplied action buffer
- consecutive admin messages become one gap fill (`beginSeq`, `newSeqNo` = first sequence *after* the skipped admins)
- each application message becomes a resend of that original sequence
- `planResend` does **not** change `nextSeq`

If `begin` is older than what the ring still holds, return `TOO_OLD`. Do not pretend you can resend an order you have overwritten. If nothing has been sent, or `begin` is past the last sent sequence, return `EMPTY`. A nonsense range returns `BAD_RANGE`.

## Constraints

- Replay is a plan, not a second sender that steals `nextSeq`. Live orders continue at the high-water. Resent application messages go out with `PossDupFlag` and their original sequence — you do not need to format the FIX text, but you do need to say that.
- Do not block the order thread on the replay. Planning reads the ring; the socket thread writes the actions.
- Admin messages are not resent verbatim. Heartbeats in the gap become a `SequenceReset` with `GapFillFlag=Y`.

## What good looks like

- Sent `APP, ADMIN, ADMIN, APP, ADMIN, APP` and a resend of 2 through 6 produces: gap-fill 2→4, resend 4, gap-fill 5→6, resend 6. `nextSeq` is still 7.
- An end sequence past `nextSeq - 1` is clamped. You cannot resend the future.
- A ring of 4, after 5 sends, answers `TOO_OLD` for sequence 1.

## Follow-ups

- What do you put in the resend of an order that has since been filled — the original new, or nothing? Who decides, the session or the OMS?
- Why is a pure in-memory ring the wrong sole store for a session that must survive a process restart at 10:15?
- How does `NextExpectedMsgSeqNum` on a logon interact with this plan?
- Where must this *not* run (inside the order thread's hot lock, which you do not have)?
