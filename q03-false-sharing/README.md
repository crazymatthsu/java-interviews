# 03 — False sharing on the order cursors

30–45 minutes. Two cores, one cache line, a slow OMS.

## Prompt

The gateway thread writes a producer sequence. The risk thread writes a consumer sequence. Both sequences are correct — counts are not lost — but when they sit on the same cache line each store invalidates the other core's line. That is false sharing, and it shows up as latency, not as a wrong fill.

Implement:

- a single-writer `Sequence` whose `value` is padded out to its own cache line (64 bytes; do not rely on `@jdk.internal.vm.annotation.Contended` unless you can say what `-XX:-RestrictContended` does)
- a `PlainCounter` with the same contract and no padding, so the difference is measurable
- a small harness that runs two threads, each adding to its own cursor

## Constraints

- Padding is not a lock. `value++` on a shared cursor is still a lost update. One writer per sequence.
- Do not "fix" this with `synchronized` or `AtomicLong`. `AtomicLong` is still one cache line if you place two of them next to each other in an array. Know that `AtomicLong` inside a padded object is fine; an array of `AtomicLong` is not.
- A test that asserts "padded is faster" will flake on a busy box. Assert correctness, and be ready to explain how you would measure the rest (taskset, isolated core, JMH, many iterations).

## What good looks like

- Seven longs (or an explicit 64-byte pad) on both sides of `value`, so the field is not on the same line as the object header of the next sequence nor the neighbouring field.
- `volatile` on the cursor that another thread reads. Padding without a memory barrier is a layout fix, not a publication fix.
- An honest statement that the result is identical with or without padding.

## Follow-ups

- Why can the JVM reorder or eliminate unused padding fields, and how do you stop that (touch them, `@Contended`, JOL)?
- What is the cache-line size on the exchange hosts you deploy to? Is it ever 128?
- Where else on an OMS does this bite (per-symbol stats array, latency histogram owned by two threads, `ByteBuffer` position)?
