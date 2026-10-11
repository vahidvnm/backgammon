package com.dara.backgammon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchRulesTest {
    @Test fun matchEndsAtOrBeyondTarget() {
        assertEquals(Player.WHITE, MatchRules.winner(5, 3, 5))
        assertEquals(Player.BLACK, MatchRules.winner(2, 7, 5))
        assertNull(MatchRules.winner(4, 4, 5))
    }

    @Test fun crawfordOccursExactlyOnceWhenAPlayerIsOneAway() {
        assertTrue(MatchRules.nextIsCrawford(4, 2, 5, crawfordUsed = false))
        assertFalse(MatchRules.nextIsCrawford(4, 2, 5, crawfordUsed = true))
        assertFalse(MatchRules.nextIsCrawford(5, 2, 5, crawfordUsed = false))
        assertFalse(MatchRules.nextIsCrawford(3, 2, 5, crawfordUsed = false))
    }
}
