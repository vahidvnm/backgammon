package com.dara.backgammon.ai

import com.dara.backgammon.core.Move
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The difficulty ladder is a pure ranking window, so it is verified here instead of
 * through playouts: a weaker level may look further down the engine's ranking, but it can
 * never invent a move, skip the engine's rules or beat the ranking of a stronger level.
 */
class DifficultyPolicyTest {
    /** Twelve distinguishable candidate sequences, ranked best first. */
    private fun ranked(count: Int = 12) = (0 until count).map { index -> listOf(Move(index, index + 1, 1)) }

    @Test fun windowsNeverWidenAsDifficultyRises() {
        val expert = DifficultyPolicy.candidateWindow(Difficulty.EXPERT)
        val hard = DifficultyPolicy.candidateWindow(Difficulty.HARD)
        val medium = DifficultyPolicy.candidateWindow(Difficulty.MEDIUM)
        val easy = DifficultyPolicy.candidateWindow(Difficulty.EASY)
        val beginner = DifficultyPolicy.candidateWindow(Difficulty.BEGINNER)
        assertEquals(1, expert)
        assertEquals(1, hard)
        assertEquals(DifficultyPolicy.MEDIUM_WINDOW, medium)
        assertEquals(DifficultyPolicy.EASY_WINDOW, easy)
        assertTrue("every level must at least consider the best move", expert <= hard && hard <= medium && medium < easy)
        assertEquals("the beginner is allowed to look at every legal sequence", 0, beginner)
    }

    @Test fun deterministicLevelsAlwaysPlayTheHighestRankedSequence() {
        val ranking = ranked()
        Difficulty.entries.filter { it == Difficulty.HARD || it == Difficulty.EXPERT }.forEach { level ->
            repeat(50) { attempt ->
                assertEquals(ranking.first(), DifficultyPolicy.choose(ranking, level, Random(attempt.toLong())))
            }
        }
    }

    @Test fun noisyLevelsStayInsideTheirOwnWindow() {
        val ranking = ranked()
        val easy = (0 until 4_000).map { DifficultyPolicy.choose(ranking, Difficulty.EASY, Random(it.toLong()))!! }
        val medium = (0 until 4_000).map { DifficultyPolicy.choose(ranking, Difficulty.MEDIUM, Random(it.toLong()))!! }
        assertTrue("EASY played outside its window", easy.all { it.first().from < DifficultyPolicy.EASY_WINDOW })
        assertTrue("MEDIUM played outside its window", medium.all { it.first().from < DifficultyPolicy.MEDIUM_WINDOW })
        assertTrue("a noisy level must not always repeat one move", easy.distinct().size > 1)
        assertTrue("a noisy level must not always repeat one move", medium.distinct().size > 1)
        assertTrue("MEDIUM must be more decisive than EASY", medium.distinct().size < easy.distinct().size)
    }

    @Test fun theBeginnerFindsTheBestMoveOnlySometimesButAlwaysALegalOne() {
        val ranking = ranked()
        val picks = (0 until 4_000).map { DifficultyPolicy.choose(ranking, Difficulty.BEGINNER, Random(it.toLong()))!! }
        assertTrue("the beginner must always return one of the engine's sequences", picks.all { it in ranking })
        assertTrue("the beginner must sometimes play the best move", picks.any { it == ranking.first() })
        assertTrue("the beginner must sometimes play something else", picks.any { it != ranking.first() })
        val guided = picks.count { it == ranking.first() }.toDouble() / picks.size
        assertTrue("beginner guidance drifted away from its declared value", kotlin.math.abs(guided - DifficultyPolicy.BEGINNER_GUIDANCE) < .05)
    }

    @Test fun aPositionWithoutASequenceHasNoChoice() {
        assertNull(DifficultyPolicy.choose(emptyList(), Difficulty.HARD, Random(1)))
        assertFalse(DifficultyPolicy.choose(emptyList(), Difficulty.BEGINNER, Random(1)) != null)
    }
}
