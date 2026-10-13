package com.dara.backgammon.ui

import androidx.compose.ui.geometry.Offset
import com.dara.backgammon.core.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardGeometryTest {
    private val geometry = BoardGeometry.from(1160f, 684f)

    @Test fun approvedMasterKeepsSixColumnsPerQuadrant() {
        val top = (0 until 12).map { column ->
            val x = if (column < 6) geometry.playLeft + (column + .5f) * geometry.columnWidth
            else geometry.barLeft + geometry.barWidth + (column - 5.5f) * geometry.columnWidth
            geometry.targetAt(Offset(x, 100f))
        }
        assertEquals((12..23).toList(), top)
        assertEquals(12, top.distinct().size)
    }

    @Test fun centreBarAndBearOffHaveIndependentHitTargets() {
        assertEquals(Move.BAR, geometry.targetAt(Offset(geometry.barLeft + geometry.barWidth / 2f, 342f)))
        assertEquals(Move.OFF, geometry.targetAt(Offset(geometry.playRight + 10f, 342f)))
    }

    @Test fun checkerCentresStayInsideCalibratedPlayingFields() {
        (0..23).forEach { point ->
            val centre = geometry.checkerCenter(point, 0)
            assertTrue(centre.x in geometry.playLeft..geometry.playRight)
            assertTrue(centre.y in geometry.rail..(geometry.height - geometry.rail))
        }
    }
}
