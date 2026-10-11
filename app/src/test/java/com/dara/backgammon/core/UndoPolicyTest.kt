package com.dara.backgammon.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoPolicyTest {
    @Test fun aiUndoStopsOnlyAtStartOfHumanTurn() {
        assertTrue(UndoPolicy.isRestoreBoundary(Mode.AI, TurnState()))
        assertFalse(UndoPolicy.isRestoreBoundary(Mode.AI, TurnState(dice = listOf(3, 2), rolled = true)))
        assertFalse(UndoPolicy.isRestoreBoundary(Mode.AI, TurnState(position = Position(turn = Player.BLACK))))
    }

    @Test fun localUndoKeepsSingleActionSemantics() {
        assertTrue(UndoPolicy.isRestoreBoundary(Mode.LOCAL, TurnState(dice = listOf(6), rolled = true)))
        assertTrue(UndoPolicy.isRestoreBoundary(Mode.LOCAL, TurnState(position = Position(turn = Player.BLACK))))
    }
}
