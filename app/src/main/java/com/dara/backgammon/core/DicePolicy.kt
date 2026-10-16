package com.dara.backgammon.core

import com.dara.backgammon.data.DoublesRate
import kotlin.random.Random

/** Fair dice generation with an explicit, symmetric reduction policy for doubles. */
object DicePolicy {
    fun doubleKeepProbability(rate: DoublesRate): Float = when (rate) {
        DoublesRate.NATURAL -> 1f
        DoublesRate.REDUCED_20 -> .8f
        DoublesRate.REDUCED_50 -> .5f
        DoublesRate.NEVER -> 0f
    }

    fun roll(rate: DoublesRate, random: Random = Random.Default): List<Int> {
        val first = random.nextInt(1, 7)
        var second = random.nextInt(1, 7)
        // Inclusive comparison: nextFloat() can return exactly 0f, which used to let a double
        // through even when the keep probability is zero. NATURAL stays safe because a
        // probability of 1f can never satisfy "value >= 1f".
        if (first == second && random.nextFloat() >= doubleKeepProbability(rate)) {
            second = random.nextInt(1, 6)
            if (second >= first) second++
        }
        return GameEngine.rollValues(first, second)
    }
}
