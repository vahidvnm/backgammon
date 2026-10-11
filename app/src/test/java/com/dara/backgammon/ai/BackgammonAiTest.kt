package com.dara.backgammon.ai

import com.dara.backgammon.core.*
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

    @Test fun aiNeverOffersTheCubeDuringCrawford() {
        val dominant = Position(points = IntArray(24).apply { this[0] = 15; this[23] = -15 }, turn = Player.BLACK)
        val match = MatchInfo(blackScore = 4, whiteScore = 2, target = 5, crawfordUsed = true, crawfordGame = true)
        assertFalse(BackgammonAi.shouldOfferDouble(dominant, Player.BLACK, match))
    }

    @Test fun matchScoreChangesCubeRiskTolerance() {
        val position = Position()
        val trailing = MatchInfo(blackScore = 1, whiteScore = 3, target = 5)
        val leading = MatchInfo(blackScore = 3, whiteScore = 1, target = 5)
        // At equal board equity a trailing player is never made more conservative than a leader.
        val trailingTake = BackgammonAi.shouldTakeDouble(position, Player.BLACK, trailing)
        val leadingTake = BackgammonAi.shouldTakeDouble(position, Player.BLACK, leading)
        assertTrue(trailingTake || !leadingTake)
    }

    @Test fun equityIsSymmetricBetweenPlayers() {
        val board = IntArray(24).apply {
            this[2] = 3; this[7] = 2; this[19] = -4; this[22] = -1
        }
        val position = Position(board, barWhite = 1, barBlack = 2, offWhite = 9, offBlack = 8)
        assertEquals(BackgammonAi.equity(position, Player.WHITE), -BackgammonAi.equity(position, Player.BLACK), 1e-9)
    }

    @Test fun hardAiTakesAnAvailableDirectHit() {
        val board = IntArray(24).apply { this[6] = 1; this[5] = -1 }
        val position = Position(board, offWhite = 14, offBlack = 14)
        val sequence = BackgammonAi.chooseSequence(position, listOf(1), Difficulty.HARD, random = Random(2))
        assertEquals(Move(6, 5, 1), sequence.single())
    }

    @Test fun everyDifficultyReturnsEngineApprovedSequencesDuringPlayouts() {
        val random = Random(918273)
        repeat(12) { gameIndex ->
            var state = TurnState()
            repeat(180) {
                if (state.winner != null) return@repeat
                val dice = GameEngine.rollValues(random.nextInt(1, 7), random.nextInt(1, 7))
                val level = Difficulty.entries[(gameIndex + it) % Difficulty.entries.size]
                val legalSequences = GameEngine.sequences(state.position, dice)
                val chosen = BackgammonAi.chooseSequence(state.position, dice, level, random = random)
                if (legalSequences.all { it.isEmpty() }) {
                    assertTrue(chosen.isEmpty())
                    state = TurnState(state.position.copy(turn = state.position.turn.other()))
                } else {
                    assertTrue("$level returned a sequence outside the rules engine", chosen in legalSequences)
                    var next = state.copy(dice = dice, rolled = true)
                    chosen.forEach { move -> next = GameEngine.afterMove(next, move) }
                    state = if (next.winner != null) next else GameEngine.endTurn(next)
                }
            }
        }
    }
}
