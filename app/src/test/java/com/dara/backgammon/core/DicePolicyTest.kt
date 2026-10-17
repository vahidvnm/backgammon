package com.dara.backgammon.core

import com.dara.backgammon.data.DoublesRate
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DicePolicyTest {
    @Test fun probabilitiesAreExplicitAndHonest() {
        assertEquals(1f, DicePolicy.doubleKeepProbability(DoublesRate.NATURAL))
        assertEquals(.8f, DicePolicy.doubleKeepProbability(DoublesRate.REDUCED_20))
        assertEquals(.5f, DicePolicy.doubleKeepProbability(DoublesRate.REDUCED_50))
        assertEquals(0f, DicePolicy.doubleKeepProbability(DoublesRate.NEVER))
    }

    @Test fun naturalDiceProduceTheStandardOneInSixDoubleRate() {
        val random = Random(20261011)
        val rolls = 120_000
        val doubles = (0 until rolls).count { DicePolicy.roll(DoublesRate.NATURAL, random).size == 4 }
        assertTrue(abs(doubles.toDouble() / rolls - 1.0 / 6.0) < .005)
    }

    @Test fun neverPolicyActuallyPreventsDoubles() {
        val random = Random(7)
        repeat(20_000) { assertEquals(2, DicePolicy.roll(DoublesRate.NEVER, random).size) }
    }

    /**
     * Regression lock for an adversarial source: [Random.nextFloat] can legitimately return
     * exactly 0f, and the old strictly-greater comparison let a double through even when the
     * keep probability was zero. A source whose bits are all zero reproduces that case.
     */
    @Test fun neverPolicySurvivesARandomSourceThatCanReturnZero() {
        val adversarial = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        assertEquals(0f, adversarial.nextFloat())
        assertEquals(2, DicePolicy.roll(DoublesRate.NEVER, adversarial).size)
    }

    @Test fun naturalPolicyStillKeepsEveryDoubleWithTheSameAdversarialSource() {
        val adversarial = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        assertEquals(4, DicePolicy.roll(DoublesRate.NATURAL, adversarial).size)
    }
}
