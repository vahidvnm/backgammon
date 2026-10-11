package com.dara.backgammon.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoPolicyTest {
    @Test fun humanRolledStatePreservesOriginalDiceForReplay() {
        assertTrue(UndoPolicy.isHumanRolledState(TurnState(dice = listOf(3, 2), rolled = true)))
    }

    @Test fun aiAndPreRollStatesAreNotReplayBoundaries() {
        assertFalse(UndoPolicy.isHumanRolledState(TurnState()))
        assertFalse(UndoPolicy.isHumanRolledState(TurnState(dice = listOf(3, 2), rolled = false)))
        assertFalse(UndoPolicy.isHumanRolledState(TurnState(position = Position(turn = Player.BLACK), dice = listOf(4, 1), rolled = true)))
    }
}
