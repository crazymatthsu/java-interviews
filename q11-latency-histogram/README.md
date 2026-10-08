# 11 — Ack-latency histogram

45 minutes. p99 without a tree and without a pause.

## Prompt

The order thread records the nanoseconds from send to ack. You need a count and an approximate percentile. You do not need a sample list.

Implement a fixed histogram:

- `record(long nanos)` — no allocation, no lock
- `percentileNanos(int percentile)` — 0 through 100, returns the lower edge of the bucket that contains that rank
- `reset()` for the next session
- 64 buckets, one per power of two. Bucket `k >= 1` is `[2^k, 2^(k+1))` nanoseconds

## Constraints

- No `List<Long>`, no HdrHistogram dependency, no `TreeMap`. You may know HdrHistogram; this question is whether you can build the smaller thing the desk actually needs.
- One writer. A second thread calling `record` is a data race, not something `synchronized` should paper over. Say which thread owns it.
- Percentile is an edge, not an interpolated fantasy. Be honest about the error (up to 2x inside a bucket) and why that is acceptable for an ack-time alarm.

## What good looks like

- Bucket index is `63 - numberOfLeadingZeros(nanos)` for nanos above 1. No loop over edges, no floating log.
- Empty histogram returns 0 rather than throwing. A bad percentile throws — that is a programmer error, not a market event.
- `reset` clears counts without allocating a new array.

## Follow-ups

- Why must the *recording* be on the order thread, and the *read* of p99 on a reporter thread, and what memory barrier does that read need?
- How do you alarm on p99 moving from 80µs to 2ms without paging a human on one outlier?
- What does a coordinated-omission bug look like if the recorder itself stalls?
