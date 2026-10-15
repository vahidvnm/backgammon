package com.dara.backgammon.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockRunPolicyTest {
    @Test fun aiChallengeChargesTheCompleteAiTurn() {
        assertTrue(ClockRunPolicy.shouldRun(Mode.AI, Player.BLACK, false, false, false))
    }

    @Test fun humanClockOnlyRunsWhileInputIsExpected() {
        assertTrue(ClockRunPolicy.shouldRun(Mode.AI, Player.WHITE, true, false, false))
        assertFalse(ClockRunPolicy.shouldRun(Mode.AI, Player.WHITE, false, false, false))
    }

    @Test fun localAnimationPausesAndTerminalOverlaysPauseBothClocks() {
        assertFalse(ClockRunPolicy.shouldRun(Mode.LOCAL, Player.BLACK, false, false, false))
        assertFalse(ClockRunPolicy.shouldRun(Mode.AI, Player.BLACK, true, true, false))
        assertFalse(ClockRunPolicy.shouldRun(Mode.AI, Player.BLACK, true, false, true))
    }
}
