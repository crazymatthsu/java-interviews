package oms.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FixOutboundTest {

    @Test
    void gapFillsAdminsAndResendsApplicationsWithoutMovingTheLiveSequence() {
        FixOutbound session = new FixOutbound(16);
        assertEquals(1L, session.send(FixOutbound.APP));
        assertEquals(2L, session.send(FixOutbound.ADMIN));
        assertEquals(3L, session.send(FixOutbound.ADMIN));
        assertEquals(4L, session.send(FixOutbound.APP));
        assertEquals(5L, session.send(FixOutbound.ADMIN));
        assertEquals(6L, session.send(FixOutbound.APP));
        assertEquals(7L, session.nextSeq());

        FixOutbound.Action[] actions = new FixOutbound.Action[8];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new FixOutbound.Action();
        }
        int n = session.planResend(2, 6, actions);
        assertEquals(4, n);
        assertGap(actions[0], 2, 4);
        assertResend(actions[1], 4);
        assertGap(actions[2], 5, 6);
        assertResend(actions[3], 6);
        assertEquals(7L, session.nextSeq());
    }

    @Test
    void clampsTheEndAndRejectsAForgottenRange() {
        FixOutbound session = new FixOutbound(4);
        for (int i = 0; i < 5; i++) {
            session.send(i % 2 == 0 ? FixOutbound.APP : FixOutbound.ADMIN);
        }
        FixOutbound.Action[] actions = new FixOutbound.Action[8];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new FixOutbound.Action();
        }
        assertEquals(FixOutbound.TOO_OLD, session.planResend(1, 4, actions));
        assertEquals(FixOutbound.BAD_RANGE, session.planResend(0, 2, actions));
        assertEquals(FixOutbound.EMPTY, new FixOutbound(4).planResend(1, 1, actions));

        assertEquals(6L, session.nextSeq());
        int n = session.planResend(2, 99, actions);
        assertEquals(4, n);
        assertGap(actions[0], 2, 3);
        assertResend(actions[1], 3);
        assertGap(actions[2], 4, 5);
        assertResend(actions[3], 5);
        assertEquals(6L, session.nextSeq());
    }

    private static void assertGap(FixOutbound.Action action, long begin, long newSeq) {
        assertEquals(FixOutbound.Action.GAP_FILL, action.type);
        assertEquals(begin, action.beginSeq);
        assertEquals(newSeq, action.newSeqNo);
    }

    private static void assertResend(FixOutbound.Action action, long seq) {
        assertEquals(FixOutbound.Action.RESEND, action.type);
        assertEquals(seq, action.msgSeq);
    }
}
