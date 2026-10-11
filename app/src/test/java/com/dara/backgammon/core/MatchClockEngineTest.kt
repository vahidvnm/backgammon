package com.dara.backgammon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchClockEngineTest {
    @Test fun activePlayerBankAndTurnTimeDecreaseTogether() {
        val result = MatchClockEngine.tick(
            ClockState(60_000, 60_000, 10_000, active = true),
            Player.WHITE, turnChanged = false, shouldRun = true,
            deltaMillis = 250, configuredTurnMillis = 10_000
        )
        assertEquals(59_750L, result.clock.whiteMillis)
        assertEquals(60_000L, result.clock.blackMillis)
        assertEquals(9_750L, result.clock.turnMillis)
    }

    @Test fun clockPausesDuringAnimationAndAiThinking() {
        val original = ClockState(60_000, 58_000, 8_000, active = true)
        val result = MatchClockEngine.tick(original, Player.BLACK, false, false, 500, 10_000)
        assertEquals(original, result.clock)
        assertFalse(result.timedOut)
        assertFalse(result.turnExpired)
    }

    @Test fun changingTurnRestoresPerTurnTimeBeforeCounting() {
        val result = MatchClockEngine.tick(
            ClockState(50_000, 40_000, 100, active = true),
            Player.BLACK, turnChanged = true, shouldRun = true,
            deltaMillis = 250, configuredTurnMillis = 10_000
        )
        assertEquals(39_750L, result.clock.blackMillis)
        assertEquals(9_750L, result.clock.turnMillis)
    }

    @Test fun bankExpiryWinsOverTurnExpiry() {
        val result = MatchClockEngine.tick(
            ClockState(100, 60_000, 100, active = true),
            Player.WHITE, false, true, 150, 10_000
        )
        assertTrue(result.timedOut)
        assertFalse(result.turnExpired)
        assertEquals(0L, result.clock.whiteMillis)
    }

    @Test fun turnExpiryRequestsForcedCompletionWithoutLosingTheMatch() {
        val result = MatchClockEngine.tick(
            ClockState(60_000, 60_000, 100, active = true),
            Player.WHITE, false, true, 150, 10_000
        )
        assertFalse(result.timedOut)
        assertTrue(result.turnExpired)
    }
}
