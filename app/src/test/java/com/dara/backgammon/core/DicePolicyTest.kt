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
}
