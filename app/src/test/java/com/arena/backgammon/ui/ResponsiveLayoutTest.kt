package com.arena.backgammon.ui

import org.junit.Assert.*
import org.junit.Test

class ResponsiveLayoutTest {
    @Test fun shortWidePhoneUsesCompactProfile() {
        val p = boardLayoutProfile(915f, 370f)
        assertTrue(p.compact)
        assertEquals(.88f, p.widthFraction)
        assertEquals(.94f, p.heightFraction)
    }

    @Test fun conventionalLandscapePhoneKeepsBalancedSurface() {
        val p = boardLayoutProfile(800f, 450f)
        assertFalse(p.compact)
        assertEquals(.76f, p.widthFraction)
        assertTrue(p.heightFraction < 1f)
    }

    @Test fun tabletDoesNotStretchBoardEdgeToEdge() {
        val p = boardLayoutProfile(1280f, 800f)
        assertEquals(.72f, p.widthFraction)
        assertEquals(.84f, p.heightFraction)
    }

    @Test fun foldableLandscapeUsesIntermediateProfile() {
        val p = boardLayoutProfile(1000f, 700f)
        assertEquals(.76f, p.widthFraction)
        assertFalse(p.compact)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidViewportIsRejected() {
        boardLayoutProfile(0f, 400f)
    }
}
