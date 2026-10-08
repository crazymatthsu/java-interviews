package oms.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class OrderFrameTest {

    @Test
    void roundTripOnADirectBufferWithoutMovingPosition() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        buffer.position(8);
        OrderFrame in = frame();
        assertEquals(OrderFrame.BYTES, OrderFrame.encode(buffer, 4, in));
        assertEquals(8, buffer.position(), "absolute puts must not touch the cursor");

        OrderFrame out = new OrderFrame();
        assertEquals(OrderFrame.BYTES, OrderFrame.decode(buffer, 4, out));
        assertEquals(in.msgType, out.msgType);
        assertEquals(in.side, out.side);
        assertEquals(in.tif, out.tif);
        assertEquals(in.flags, out.flags);
        assertEquals(in.symbolId, out.symbolId);
        assertEquals(in.clOrdId, out.clOrdId);
        assertEquals(in.qty, out.qty);
        assertEquals(in.priceTicks, out.priceTicks);
    }

    @Test
    void packsTwoFramesAndRejectsAShortTail() {
        ByteBuffer buffer = ByteBuffer.allocate(OrderFrame.BYTES + 8);
        OrderFrame first = frame();
        OrderFrame second = frame();
        second.clOrdId = 99;
        second.msgType = OrderFrame.CANCEL;
        assertEquals(OrderFrame.BYTES, OrderFrame.encode(buffer, 0, first));
        assertEquals(-1, OrderFrame.encode(buffer, OrderFrame.BYTES, second));
        assertEquals(-1, OrderFrame.decode(buffer, OrderFrame.BYTES, new OrderFrame()));
        assertEquals(-1, OrderFrame.encode(buffer, 0, null));
    }

    private static OrderFrame frame() {
        OrderFrame order = new OrderFrame();
        order.msgType = OrderFrame.NEW;
        order.side = OrderFrame.BUY;
        order.tif = OrderFrame.IOC;
        order.flags = 0;
        order.symbolId = 1_024;
        order.clOrdId = 88_001L;
        order.qty = 500;
        order.priceTicks = 1_502_500L;
        return order;
    }
}
