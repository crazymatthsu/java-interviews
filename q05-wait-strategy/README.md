# 05 — Spin, then park, the order sequencer

45 minutes. The order thread is quiet between waves and must not sleep through a new order.

## Prompt

A consumer waits until a sequence reaches a target. Under load it should poll. When the gateway is idle it should not burn a core forever, and it must not miss a publish that happens just as it goes to sleep.

Implement:

- `void publish(long sequence)` — release-store, then unpark a waiter if one is registered
- `long await(long target, long timeoutNanos)` — returns the observed sequence, or `-1` on timeout
- three phases: `Thread.onSpinWait`, then `Thread.yield`, then `LockSupport.parkNanos`
- sequences are non-negative; `-1` means timeout only

## Constraints

- No `Object.wait` / `notify`, no `Condition`, no `BlockingQueue`.
- Fix the missed-wakeup race: the waiter must store its `Thread` *before* it re-reads the sequence, and only then park. Draw the interleaving.
- Do not reach for virtual threads. They are for blocking IO fan-out (a FIX session pool, a drop-copy writer). The thread that sequences orders stays on a platform thread.

## What good looks like

- `publish` is wait-free aside from the unpark.
- `await` returns immediately when the sequence is already at the target — no park.
- Interrupt policy is explicit. This solution restores the interrupt flag and returns timeout rather than throwing out of the order loop.

## Follow-ups

- How does Disruptor's `BusySpinWaitStrategy`, `YieldingWaitStrategy` and `BlockingWaitStrategy` map onto what you wrote?
- What latency do you add by parking, and when is a dedicated spinning core cheaper than that tail?
- Why is `LockSupport.park` inside a virtual thread a different product decision from parking the sequencer?
