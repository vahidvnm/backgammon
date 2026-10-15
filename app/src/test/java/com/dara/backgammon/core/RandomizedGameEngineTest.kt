package com.dara.backgammon.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomizedGameEngineTest {
    @Test fun deterministicRandomPlayoutsPreserveAllCheckersAndLegality() {
        val random = Random(20261011)
        repeat(100) {
            var state = TurnState()
            repeat(400) {
                if (state.winner != null) return@repeat
                if (!state.rolled) {
                    state = state.copy(
                        dice = GameEngine.rollValues(random.nextInt(1, 7), random.nextInt(1, 7)),
                        rolled = true
                    )
                }
                val legal = GameEngine.legalMoves(state.position, state.dice)
                state = if (legal.isEmpty()) GameEngine.endTurn(state)
                else GameEngine.afterMove(state, legal.random(random))
                assertConserved(state.position)
            }
        }
    }

    @Test fun everyOpeningRollHasACompleteLegalContinuation() {
        val opening = Position()
        for (first in 1..6) for (second in 1..6) {
            val dice = GameEngine.rollValues(first, second)
            val sequences = GameEngine.sequences(opening, dice)
            assertTrue(sequences.isNotEmpty())
            val used = sequences.first().map { it.die }
            assertTrue(used.all { it in dice })
        }
    }

    private fun assertConserved(position: Position) {
        val white = position.points.sumOf { it.coerceAtLeast(0) } + position.barWhite + position.offWhite
        val black = position.points.sumOf { (-it).coerceAtLeast(0) } + position.barBlack + position.offBlack
        assertEquals(15, white)
        assertEquals(15, black)
        assertTrue(position.points.all { it in -15..15 })
    }
}
