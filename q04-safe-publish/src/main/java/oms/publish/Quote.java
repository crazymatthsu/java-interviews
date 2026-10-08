package oms.publish;

/** Immutable NBBO. Safe to publish by a single reference write. */
public record Quote(
        int symbolId,
        long bidTicks,
        long askTicks,
        long bidQty,
        long askQty,
        long sequence) {
}
