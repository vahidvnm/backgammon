package com.dara.backgammon.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSessionValidatorTest {
    private fun session(
        game: TurnState = TurnState(),
        match: MatchInfo = MatchInfo(),
        clock: ClockState = ClockState()
    ) = SavedSession(game, match, clock, Mode.AI, autoAssist = false)

    @Test fun acceptsAConsistentProcessDeathSnapshot() {
        assertTrue(SavedSessionValidator.isValid(session()))
    }

    @Test fun rejectsMissingOrInventedCheckers() {
        val points = Position.initialPoints().also { it[23]-- }
        assertFalse(SavedSessionValidator.isValid(session(game = TurnState(Position(points)))))
    }

    @Test fun rejectsUnconfirmedDiceFromAnInterruptedRoll() {
        assertFalse(SavedSessionValidator.isValid(session(game = TurnState(dice = listOf(3, 4), rolled = false))))
    }

    @Test fun acceptsARealRolledTurnButRejectsImpossibleDice() {
        assertTrue(SavedSessionValidator.isValid(session(game = TurnState(dice = listOf(3, 4), rolled = true))))
        assertFalse(SavedSessionValidator.isValid(session(game = TurnState(dice = listOf(0, 7), rolled = true))))
    }

    @Test fun rejectsInvalidCubeAndCrawfordCombinations() {
        assertFalse(SavedSessionValidator.isValid(session(match = MatchInfo(cube = 3))))
        assertFalse(SavedSessionValidator.isValid(session(match = MatchInfo(crawfordGame = true, crawfordUsed = false))))
        assertTrue(SavedSessionValidator.isValid(session(match = MatchInfo(crawfordGame = true, crawfordUsed = true))))
    }

    @Test fun rejectsNegativeClockValues() {
        assertFalse(SavedSessionValidator.isValid(session(clock = ClockState(whiteMillis = -1))))
    }
}
