package com.dara.backgammon.ui

import androidx.compose.ui.geometry.Offset
import com.dara.backgammon.core.Move
import kotlin.math.min

/**
 * Single calibrated geometry source shared by drawing and pointer input.
 * Fractions follow the approved 1160x684 board master, including its centre bar and bear-off tray.
 */
data class BoardGeometry private constructor(
    val width: Float,
    val height: Float,
    val rail: Float,
    val barWidth: Float,
    val playLeft: Float,
    val playRight: Float,
    val half: Float,
    val barLeft: Float,
    val columnWidth: Float
) {
    fun targetAt(position: Offset): Int {
        if (position.x > playRight) return Move.OFF
        if (position.x in barLeft..(barLeft + barWidth)) return Move.BAR
        val local = if (position.x < barLeft) position.x - playLeft
        else position.x - (barLeft + barWidth) + half
        val column = (local / columnWidth).toInt().coerceIn(0, 11)
        return if (position.y < height / 2f) 12 + column else 11 - column
    }

    fun checkerCenter(point: Int, index: Int): Offset {
        val column = if (point < 12) 11 - point else point - 12
        val x = if (column < 6) playLeft + (column + .5f) * columnWidth
        else barLeft + barWidth + (column - 5.5f) * columnWidth
        val radius = columnWidth * .262f
        val gap = min(radius * 1.72f, (height * .37f) / 5f)
        return Offset(x, if (point >= 12) rail + radius + index.coerceAtMost(4) * gap
        else height - rail - radius - index.coerceAtMost(4) * gap)
    }

    companion object {
        fun from(width: Float, height: Float): BoardGeometry {
            require(width > 0f && height > 0f)
            val rail = width * .032f
            val barWidth = width * .062f
            val playLeft = rail
            val playRight = width - width * .060f
            val half = (playRight - playLeft - barWidth) / 2f
            return BoardGeometry(width, height, rail, barWidth, playLeft, playRight,
                half, playLeft + half, half / 6f)
        }
    }
}
