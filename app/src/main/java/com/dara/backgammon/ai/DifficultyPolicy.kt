package com.dara.backgammon.ai

import com.dara.backgammon.core.Move
import kotlin.random.Random

/**
 * Purely deterministic difficulty ladder, kept out of [BackgammonAi] so the strength
 * policy can be unit tested without a board, a clock, a coroutine or Android.
 *
 * Every level receives the engine's legal sequences already ordered from best to worst.
 * A level is defined by how far down that ranking it is allowed to look, and the beginner
 * level additionally plays at random unless its guidance roll succeeds. Windows are only a
 * ranking window over legal sequences, so a weaker level can never leave the rules engine.
 *
 * EXPERT keeps its window at one as well; its extra strength comes from the reply search in
 * [BackgammonAi], which needs the board that this policy deliberately knows nothing about.
 */
object DifficultyPolicy {
    /** The beginner plays a random legal move unless its guidance roll succeeds. */
    const val BEGINNER_GUIDANCE = .5f
    /** EASY looks this far down the ranking before picking at random. */
    const val EASY_WINDOW = 8
    /** MEDIUM looks this far down the ranking before picking at random. */
    const val MEDIUM_WINDOW = 4

    /**
     * Sequences a level may choose from, `0` meaning "any legal sequence".
     * Monotone by design: a lower difficulty never has a narrower window.
     */
    fun candidateWindow(level: Difficulty): Int = when (level) {
        Difficulty.BEGINNER -> 0
        Difficulty.EASY -> EASY_WINDOW
        Difficulty.MEDIUM -> MEDIUM_WINDOW
        Difficulty.HARD, Difficulty.EXPERT -> 1
    }

    /**
     * Picks one of [rankedSequences] (best first). Returns `null` when the position has no
     * legal sequence, which the caller must treat as "this turn cannot move".
     */
    fun choose(rankedSequences: List<List<Move>>, level: Difficulty, random: Random): List<Move>? {
        if (rankedSequences.isEmpty()) return null
        val window = candidateWindow(level)
        if (window == 0) {
            val guided = random.nextFloat() < BEGINNER_GUIDANCE
            return if (guided) rankedSequences.first() else rankedSequences.random(random)
        }
        if (window == 1) return rankedSequences.first()
        return rankedSequences.take(window).random(random)
    }
}
