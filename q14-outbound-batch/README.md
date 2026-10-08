# 14 — Outbound batch and a short write

45 minutes. The socket will not take the whole buffer.

## Prompt

The order thread appends already-encoded frames. The IO thread writes them. A TCP write can accept fewer bytes than you offered. The remainder must stay, in order, without allocating a new buffer.

Implement:

- `boolean tryAppend(byte[] frame, int off, int len)` — false when the staging buffer cannot hold the whole frame (do not split a frame on append, and do not block)
- `int flush(ByteSink sink)` — sink returns how many bytes it took, including zero. Compact the tail to the front
- no allocation on append or flush

## Constraints

- A sink that returns more than the offered length, or a negative count, is a broken IO thread: throw. A sink that returns zero is backpressure: keep the bytes.
- Do not use `ByteArrayOutputStream`. Do not use a `Queue<byte[]>` of one array per order.
- Append is all-or-nothing per frame so you never flush a torn header. Flush *may* split a frame across socket writes; the byte stream is still ordered.

## What good looks like

- `free()` tells the producer whether the next frame fits before it encodes into a scratch buffer.
- After a 3-byte write of a 7-byte batch, `size()` is 4 and the next flush starts at the first unwritten byte.
- When the buffer is full, `tryAppend` returns false and the bytes already staged are unchanged.

## Follow-ups

- Why is compacting with `System.arraycopy` fine here, and when would you switch to a ring with a gather write?
- What is the failure mode if the producer blocks on a full buffer instead of shedding?
- How do you bound the time the IO thread spends in `flush` if the sink is greedy?
