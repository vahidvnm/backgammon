package com.dara.backgammon.ui

import androidx.compose.ui.geometry.Offset
import com.dara.backgammon.core.Move
import com.dara.backgammon.data.BoardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the per-theme artwork calibration: a tap on any rendered point must resolve to that
 * point, stacks must stay inside the baked triangles, and the bar/bear-off lanes must be
 * independent. Every theme is exercised at both the native master aspect and a wide phone.
 */
class BoardGeometryTest {
    private val themes = BoardTheme.entries.toList()

    private fun geometryFor(theme: BoardTheme, width: Float, height: Float) =
        BoardGeometry.from(width, height, theme)

    @Test fun everyThemeMapsEachColumnCentreToItsOwnPoint() {
        themes.forEach { theme ->
            val geometry = geometryFor(theme, 1160f, 684f)
            (0 until 24).forEach { point ->
                val x = geometry.columnCenterX(point)
                val y = if (point >= 12) 100f else 584f
                assertEquals("$theme point $point", point, geometry.targetAt(Offset(x, y)))
                // Half a pitch either way still belongs to the same column of points.
                val drift = geometry.calib.averagePitch * 1160f * .38f
                assertEquals("$theme point $point left", point, geometry.targetAt(Offset(x - drift, y)))
                assertEquals("$theme point $point right", point, geometry.targetAt(Offset(x + drift, y)))
            }
        }
    }

    @Test fun centreBarAndBearOffKeepIndependentHitTargets() {
        themes.forEach { theme ->
            val geometry = geometryFor(theme, 1160f, 684f)
            assertEquals(Move.BAR, geometry.targetAt(Offset(geometry.barLeft + geometry.barWidth / 2f, 342f)))
            assertEquals(Move.OFF, geometry.targetAt(Offset(geometry.playRight + 10f, 342f)))
        }
    }

    @Test fun stacksSitOnTheirArtworkBasesAndStayUnderTheTipsAtAnyAspect() {
        val aspects = listOf(1160f / 684f, 1160f / 592f, 1160f / 760f)
        themes.forEach { theme ->
            val calib = BoardGeometry.from(1160f, 684f, theme).calib
            aspects.forEach { aspect ->
                val height = 1160f / aspect
                val geometry = geometryFor(theme, 1160f, height)
                (0..23).forEach { point ->
                    val radius = geometry.columnWidth * .262f
                    val first = geometry.checkerCenter(point, 0)
                    val fifth = geometry.checkerCenter(point, 4)
                    assertTrue("$theme stack escapes the playfield at ${aspect}",
                        first.x in geometry.playLeft..geometry.playRight)
                    if (point >= 12) {
                        assertEquals(first.y, height * calib.topBase + radius, 0.6f)
                        assertTrue("$theme top row overshoots the tip (${theme})",
                            fifth.y + radius <= height * calib.topApex + radius * 2.05f)
                        assertTrue(fifth.y > first.y)
                    } else {
                        assertEquals(first.y, height * calib.botBase - radius, 0.6f)
                        assertTrue("$theme bottom row overshoots the tip (${theme})",
                            fifth.y - radius >= height * calib.botApex - radius * 2.05f)
                        assertTrue(fifth.y < first.y)
                    }
                }
            }
        }
    }

    @Test fun trayRectsMatchTheCarvedGrooves() {
        val geometry = geometryFor(BoardTheme.ROYAL_SAPPHIRE, 1160f, 684f)
        val top = geometry.trayRect(true)
        assertTrue(top.left > geometry.playRight)
        assertTrue(top.right <= 1160f)
        assertTrue(top.top > 0f && top.bottom < 684f * .62f)
        val bottom = geometry.trayRect(false)
        assertTrue(bottom.top > 342f && bottom.bottom < 684f)
    }
}
