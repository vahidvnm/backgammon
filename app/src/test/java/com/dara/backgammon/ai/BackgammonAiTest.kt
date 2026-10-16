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

    /**
     * A contested midgame sample used as a fixture for the personality styles. No checker is
     * on the bar, so the roll has real choices instead of a forced entry.
     */
    private fun contestedPosition() = Position(
        points = intArrayOf(0, 3, 0, -1, 0, 4, 0, 1, 0, 0, 0, -3, 3, -1, 2, 0, -2, -2, -2, 0, 0, 2, -2, -2),
        barWhite = 0, barBlack = 0, offWhite = 0, offBlack = 0, turn = Player.WHITE
    )

    /** The styled personalities must reach the evaluation function, not just the signature. */
    @Test fun personalityStylesChangeTheChosenMoveOnAContestedPosition() {
        val position = contestedPosition()
        val dice = listOf(3, 1)
        fun pick(persona: AiPersona) =
            BackgammonAi.chooseSequence(position, dice, Difficulty.HARD, persona, Random(5))

        // BUILDER makes the point on 3, HUNTER pushes a checker into white's home board,
        // RUNNER and MASTER keep the flexible outer split. Pinned so a style regression
        // names itself instead of silently degrading the advertised personalities.
        assertEquals(listOf(Move(7, 6, 1), Move(6, 3, 3)), pick(AiPersona.BUILDER))
        assertEquals(listOf(Move(14, 13, 1), Move(5, 2, 3)), pick(AiPersona.HUNTER))
        assertEquals(listOf(Move(14, 13, 1), Move(7, 4, 3)), pick(AiPersona.RUNNER))
        assertEquals(listOf(Move(14, 13, 1), Move(7, 4, 3)), pick(AiPersona.MASTER))
        assertTrue("styles must not be cosmetic", pick(AiPersona.BUILDER) != pick(AiPersona.HUNTER))
        assertTrue("styles must not be cosmetic", pick(AiPersona.RUNNER) != pick(AiPersona.HUNTER))
    }

    /**
     * The same roll must always produce the same answer. Before the search canonicalised the
     * dice order, a warm cache filled from the mirrored roll could flip the tie between two
     * equal-scoring routes, making an apparently pure function order-dependent.
     */
    @Test fun theSameRollAlwaysProducesTheSameChoiceRegardlessOfCacheOrder() {
        val position = contestedPosition()
        BackgammonAi.clearSequenceCache()
        val rolledThreeFirst = BackgammonAi.chooseSequence(position, listOf(3, 1), Difficulty.HARD)
        val rolledOneFirst = BackgammonAi.chooseSequence(position, listOf(1, 3), Difficulty.HARD)
        assertEquals(rolledThreeFirst, rolledOneFirst)

        BackgammonAi.clearSequenceCache()
        val coldOneFirst = BackgammonAi.chooseSequence(position, listOf(1, 3), Difficulty.HARD)
        val warmThreeFirst = BackgammonAi.chooseSequence(position, listOf(3, 1), Difficulty.HARD)
        assertEquals(coldOneFirst, warmThreeFirst)
    }

    /** MASTER is the shipped opponent, so its choices may never drift from the default. */
    @Test fun masterPersonalityIsTheUnchangedDefaultOpponent() {
        (1..6).forEach { first ->
            (first..6).forEach { second ->
                val dice = GameEngine.rollValues(first, second)
                val default = BackgammonAi.chooseSequence(contestedPosition(), dice, Difficulty.HARD, random = Random(1))
                val explicit = BackgammonAi.chooseSequence(contestedPosition(), dice, Difficulty.HARD, AiPersona.MASTER, Random(1))
                assertEquals("MASTER must behave exactly like the default opponent", default, explicit)
            }
        }
    }

    /** Every style must stay inside the rules engine on both sides of the board. */
    @Test fun everyPersonalityPlaysLegalSequencesAsBlackToo() {
        val random = Random(4242)
        repeat(40) {
            val position = Position(
                points = intArrayOf(0, 3, 0, -1, 0, 4, 0, 1, 0, 0, 0, -3, 3, -1, 2, 0, -2, -2, -2, 0, 0, 2, -2, -2),
                turn = Player.BLACK
            )
            val dice = GameEngine.rollValues(random.nextInt(1, 7), random.nextInt(1, 7))
            AiPersona.entries.forEach { persona ->
                val sequence = BackgammonAi.chooseSequence(position, dice, Difficulty.entries.random(random), persona, random)
                var current = position
                sequence.forEach { move ->
                    assertTrue("$persona produced an illegal move $move", move in GameEngine.rawMoves(current, move.die))
                    current = GameEngine.apply(current, move)
                }
            }
        }
    }

    @Test fun legalSequenceCacheReusesEquivalentDiceOrders() {
        BackgammonAi.clearSequenceCache()
        val first = BackgammonAi.chooseSequence(Position(), listOf(3, 1), Difficulty.HARD, random = Random(1))
        val sizeAfterFirst = BackgammonAi.sequenceCacheSize()
        val second = BackgammonAi.chooseSequence(Position(), listOf(1, 3), Difficulty.HARD, random = Random(1))
        assertEquals(first.toSet(), second.toSet())
        assertEquals(sizeAfterFirst, BackgammonAi.sequenceCacheSize())
        assertEquals(1, sizeAfterFirst)
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
