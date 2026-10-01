package com.dara.backgammon.core

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun position(
        white: Map<Int, Int> = emptyMap(),
        black: Map<Int, Int> = emptyMap(),
        barWhite: Int = 0,
        barBlack: Int = 0,
        offWhite: Int = 0,
        offBlack: Int = 0,
        turn: Player = Player.WHITE
    ): Position {
        val board = IntArray(24)
        white.forEach { (point, count) -> board[point] = count }
        black.forEach { (point, count) -> board[point] = -count }
        return Position(board, barWhite, barBlack, offWhite, offBlack, turn)
    }

    @Test fun initialPositionHasFifteenCheckersPerPlayer() {
        val p = Position()
        assertEquals(15, p.points.sumOf { it.coerceAtLeast(0) })
        assertEquals(15, p.points.sumOf { (-it).coerceAtLeast(0) })
    }

    @Test fun doublesExpandToFourDice() {
        assertEquals(listOf(5, 5, 5, 5), GameEngine.rollValues(5, 5))
        assertEquals(listOf(2, 6), GameEngine.rollValues(2, 6))
    }

    @Test fun barEntryIsMandatoryBeforeBoardMoves() {
        val p = position(white = mapOf(10 to 1), barWhite = 1)
        assertTrue(GameEngine.rawMoves(p, 1).all { it.from == Move.BAR })
    }

    @Test fun blockedBarEntryProducesNoMove() {
        val p = position(black = mapOf(23 to 2), barWhite = 1)
        assertTrue(GameEngine.rawMoves(p, 1).isEmpty())
    }

    @Test fun hittingABlotMovesOpponentToBar() {
        val p = GameEngine.apply(position(white = mapOf(6 to 1), black = mapOf(5 to 1)), Move(6, 5, 1))
        assertEquals(1, p.barBlack)
        assertEquals(1, p.points[5])
    }

    @Test fun pointWithTwoOpponentsIsBlocked() {
        val p = position(white = mapOf(6 to 1), black = mapOf(5 to 2))
        assertTrue(GameEngine.rawMoves(p, 1).none { it.from == 6 && it.to == 5 })
    }

    @Test fun maximumDiceUsageIsEnforced() {
        val p = position(white = mapOf(3 to 1, 1 to 1), offWhite = 13)
        val sequences = GameEngine.sequences(p, listOf(1, 2))
        assertTrue(sequences.isNotEmpty())
        val maximum = sequences.maxOf { it.size }
        assertTrue(sequences.all { it.size == maximum })
    }

    @Test fun higherDieIsRequiredWhenOnlyOneCanBePlayed() {
        // Both dice can move the checker initially, but point 2 blocks every follow-up.
        val p = position(white = mapOf(5 to 1), black = mapOf(2 to 2), offWhite = 14)
        val legal = GameEngine.legalMoves(p, listOf(1, 2))
        assertEquals(listOf(Move(5, 3, 2)), legal)
    }

    @Test fun exactBearOffIsLegal() {
        val p = position(white = mapOf(0 to 1), offWhite = 14)
        assertTrue(Move(0, Move.OFF, 1) in GameEngine.rawMoves(p, 1))
    }

    @Test fun oversizedBearOffOnlyUsesFarthestChecker() {
        val p = position(white = mapOf(3 to 1, 1 to 1), offWhite = 13)
        assertTrue(Move(3, Move.OFF, 6) in GameEngine.rawMoves(p, 6))
        assertFalse(Move(1, Move.OFF, 6) in GameEngine.rawMoves(p, 6))
    }

    @Test fun cannotBearOffUntilEveryCheckerIsHome() {
        val p = position(white = mapOf(8 to 1, 0 to 1), offWhite = 13)
        assertTrue(GameEngine.rawMoves(p, 1).none { it.to == Move.OFF })
    }

    @Test fun fourMovesCanBeConsumedFromADouble() {
        val p = position(white = mapOf(7 to 1), offWhite = 14)
        assertEquals(4, GameEngine.sequences(p, listOf(2, 2, 2, 2)).single().size)
    }

    @Test fun movePlansDescribeDirectAndCompoundDiceRoutes() {
        val p = position(white = mapOf(7 to 1), offWhite = 14)
        val plans = GameEngine.movePlans(p, listOf(2, 3), 7)
        assertTrue(plans.any { it.to == 5 && it.dice == listOf(2) })
        assertTrue(plans.any { it.to == 2 && it.dice.sum() == 5 && it.moves.size == 2 })
    }

    @Test fun movePlansNeverJumpToAnotherChecker() {
        val p = position(white = mapOf(7 to 1, 4 to 1), offWhite = 13)
        assertTrue(GameEngine.movePlans(p, listOf(3, 2), 7).all { plan ->
            plan.moves.zipWithNext().all { (a, b) -> a.to == b.from }
        })
    }

    @Test fun turnCannotEndWhileALegalMoveRemains() {
        val state = TurnState(position(white = mapOf(5 to 1), offWhite = 14), listOf(1), rolled = true)
        assertThrows(IllegalArgumentException::class.java) { GameEngine.endTurn(state) }
    }

    @Test fun winnerIsDeclaredAtFifteenBorneOff() {
        val state = TurnState(position(white = mapOf(0 to 1), offWhite = 14), listOf(1), rolled = true)
        assertEquals(Player.WHITE, GameEngine.afterMove(state, Move(0, Move.OFF, 1)).winner)
    }
}
