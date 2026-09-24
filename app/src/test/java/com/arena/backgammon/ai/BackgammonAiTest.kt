package com.arena.backgammon.ai

import com.arena.backgammon.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class BackgammonAiTest {
    @Test fun everyPersonaReturnsACompleteLegalSequence() {
        val position = Position()
        AiPersona.entries.forEach { persona ->
            val sequence = BackgammonAi.chooseSequence(position, listOf(3, 1), Difficulty.HARD, persona, Random(7))
            assertTrue("$persona should find a move", sequence.isNotEmpty())
            var current = position
            sequence.forEach { move ->
                assertTrue(move in GameEngine.rawMoves(current, move.die))
                current = GameEngine.apply(current, move)
            }
        }
    }

    @Test fun personasRespectACompletelyBlockedBar() {
        val board = IntArray(24).apply { this[23] = -2; this[22] = -2 }
        val position = Position(points = board, barWhite = 1)
        AiPersona.entries.forEach { persona ->
            assertTrue(BackgammonAi.chooseSequence(position, listOf(1, 2), Difficulty.EXPERT, persona).isEmpty())
        }
    }
}
