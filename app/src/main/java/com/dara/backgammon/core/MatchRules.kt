package com.dara.backgammon.core

/** Standard match-level rules independent from Android UI state. */
object MatchRules {
    fun winner(whiteScore: Int, blackScore: Int, target: Int): Player? = when {
        whiteScore >= target -> Player.WHITE
        blackScore >= target -> Player.BLACK
        else -> null
    }

    /** Crawford applies exactly once, in the first game after either player becomes one-away. */
    fun nextIsCrawford(whiteScore: Int, blackScore: Int, target: Int, crawfordUsed: Boolean): Boolean =
        !crawfordUsed && winner(whiteScore, blackScore, target) == null &&
            (whiteScore == target - 1 || blackScore == target - 1)
}
