package oms.codec;

import java.nio.ByteBuffer;

/**
 * 32-byte child-order frame. Big-endian, fixed offsets, no allocation.
 *
 * <pre>
 *  0  int8  msgType
 *  1  int8  side
 *  2  int8  tif
 *  3  int8  flags
 *  4  int32 symbolId
 *  8  int64 clOrdId
 * 16  int64 qty
 * 24  int64 priceTicks
 * </pre>
 *
 * Encode and decode return {@code -1} instead of throwing when the buffer
 * cannot hold a frame. The hot path does not allocate an exception.
 */
public final class OrderFrame {
    public static final int BYTES = 32;

    public static final byte NEW = 1;
    public static final byte CANCEL = 2;
    public static final byte REPLACE = 3;

    public static final byte BUY = 1;
    public static final byte SELL = 2;

    public static final byte DAY = 1;
    public static final byte IOC = 2;

    public byte msgType;
    public byte side;
    public byte tif;
    public byte flags;
    public int symbolId;
    public long clOrdId;
    public long qty;
    public long priceTicks;

    public static int encode(ByteBuffer buffer, int index, OrderFrame order) {
        if (order == null || index < 0 || buffer.capacity() - index < BYTES) {
            return -1;
        }
        buffer.put(index, order.msgType);
        buffer.put(index + 1, order.side);
        buffer.put(index + 2, order.tif);
        buffer.put(index + 3, order.flags);
        buffer.putInt(index + 4, order.symbolId);
        buffer.putLong(index + 8, order.clOrdId);
        buffer.putLong(index + 16, order.qty);
        buffer.putLong(index + 24, order.priceTicks);
        return BYTES;
    }

    public static int decode(ByteBuffer buffer, int index, OrderFrame into) {
        if (into == null || index < 0 || buffer.capacity() - index < BYTES) {
            return -1;
        }
        into.msgType = buffer.get(index);
        into.side = buffer.get(index + 1);
        into.tif = buffer.get(index + 2);
        into.flags = buffer.get(index + 3);
        into.symbolId = buffer.getInt(index + 4);
        into.clOrdId = buffer.getLong(index + 8);
        into.qty = buffer.getLong(index + 16);
        into.priceTicks = buffer.getLong(index + 24);
        return BYTES;
    }
}
