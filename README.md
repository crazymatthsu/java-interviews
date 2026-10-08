# Java interviews

Senior Java screen for an **equities sell-side trading OMS**. The order path is high-frequency in the desk sense: predictable microseconds, no allocation and no lock on the send thread, explicit memory ordering, FIX that stays correct while the live sequence keeps moving. It is not a generic collections quiz, and it is not a kernel-bypass matching-engine exercise.

Fifteen independent Gradle subprojects. Language level **17**. JUnit 5. No third-party libraries on the main classpath — the point of each round is the JDK code you would actually write. The prompt is the module `README.md`. The reference solution is `src/main`, covered by `src/test`.

```bash
gradle test
```

| # | Module | Interview question | Focus |
|---|---|---|---|
| 01 | [q01-spsc-ring](q01-spsc-ring/) | Hand child-order ids from the gateway thread to the single risk thread, in order, with no locks and no allocation. | SPSC ring, release/acquire |
| 02 | [q02-order-pool](q02-order-pool/) | Recycle order tickets so the send path never allocates, and reject a double-free. | Object pool, ABA generation |
| 03 | [q03-false-sharing](q03-false-sharing/) | Keep the producer and consumer cursors off the same cache line. | False sharing, padding |
| 04 | [q04-safe-publish](q04-safe-publish/) | Publish an immutable NBBO so readers never observe a torn quote. | VarHandle release/acquire |
| 05 | [q05-wait-strategy](q05-wait-strategy/) | Spin while the sequencer is hot, park when it is idle, and do not miss the wake-up. | onSpinWait, LockSupport |
| 06 | [q06-binary-codec](q06-binary-codec/) | Encode a child order as a 32-byte frame on a ByteBuffer without allocating. | Binary layout, flyweight |
| 07 | [q07-tick-math](q07-tick-math/) | Do price, tick alignment and notional in integer ticks. No BigDecimal, no double. | Fixed point, overflow |
| 08 | [q08-limit-book](q08-limit-book/) | Maintain best bid and offer for one equity on a dense price ladder. | Level book, O(1) touch |
| 09 | [q09-pretrade-risk](q09-pretrade-risk/) | Reject fat-finger, collar, position and duplicate client orders before they are sequenced. | Pre-trade risk, open addressing |
| 10 | [q10-symbol-intern](q10-symbol-intern/) | Map ticker bytes to a dense id. Lookup must not allocate. | Intern table |
| 11 | [q11-latency-histogram](q11-latency-histogram/) | Record gateway ack latency and answer a percentile without allocating. | Log histogram |
| 12 | [q12-order-fsm](q12-order-fsm/) | Enforce the child-order lifecycle. Fills and cancels have to conserve quantity. | Single-writer state machine |
| 13 | [q13-quote-coalesce](q13-quote-coalesce/) | Coalesce NBBO quotes to the latest value. Never drop a print to keep up. | Market-data fan-out |
| 14 | [q14-outbound-batch](q14-outbound-batch/) | Batch outbound frames and keep order when the socket accepts a short write. | Backpressure, partial write |
| 15 | [q15-fix-resend](q15-fix-resend/) | Answer a FIX ResendRequest with gap fills. Do not move the live send sequence. | FIX session, replay buffer |

## How to read a module

Each subproject stands alone. Start with its README (the question a candidate sees), then the reference solution, then the tests. Follow-ups at the bottom of each README are the second half of the interview.

Shared rules across the set:

- The order thread is single-writer. Do not add `synchronized` to make a data race "safe".
- A reject on the hot path is a reason code, not an exception. Exceptions are for broken callers.
- `String`, `BigDecimal`, `HashMap` and `ArrayBlockingQueue` are edge or admin tools. Say why before you put one on the send path.
- Virtual threads are for blocking IO fan-out. They are the wrong tool for the sequencer.

## Layout

```
settings.gradle
build.gradle          # Java 17, JUnit 5, applied to every subproject
q01-spsc-ring/
  README.md
  src/main/java/...
  src/test/java/...
...
q15-fix-resend/
```
