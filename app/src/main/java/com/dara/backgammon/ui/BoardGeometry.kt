package com.dara.backgammon.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.dara.backgammon.core.Move
import com.dara.backgammon.data.BoardTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Physical layout of one board master, measured from the rendered artwork so checkers,
 * tap targets and reticles sit exactly on the baked triangles at *any* canvas aspect.
 *
 * X values are fractions of canvas width, Y values are fractions of canvas height; the
 * master image is always stretched to fill the canvas, so fractional anchors survive the
 * aspect change while width-based rails (the pre-1.3.8 design) drifted into the frame bands
 * on wide phones and tablets.
 *  - [leftOrigin]/[leftPitch]: centre of the leftmost bottom/top column and column pitch.
 *  - [rightOrigin]/[rightPitch]: centre of the first right-half column and its pitch.
 *  - [topBase]/[topApex]: first checker contact line and tip of the downward triangles.
 *  - [botBase]/[botApex]: the same for the upward triangles, [botBase] measured from the top.
 *  - tray rectangles reproduce the bear-off grooves carved into each frame rail.
 */
data class BoardCalibration(
    val leftOrigin: Float,
    val leftPitch: Float,
    val rightOrigin: Float,
    val rightPitch: Float,
    val topBase: Float,
    val topApex: Float,
    val botBase: Float,
    val botApex: Float,
    val trayLeft: Float,
    val trayWidth: Float,
    val topTrayTop: Float,
    val topTrayBottom: Float,
    val bottomTrayTop: Float,
    val bottomTrayBottom: Float
) {
    /** Checker centre columns never spill onto the frame: playfield ends half a pitch out. */
    val playLeft get() = leftOrigin - leftPitch / 2f
    val playRight get() = rightOrigin + 5.5f * rightPitch
    val barLeft get() = leftOrigin + 5.5f * leftPitch
    val barRight get() = rightOrigin - 0.5f * rightPitch
    val averagePitch get() = (leftPitch + rightPitch) / 2f
}

private fun boardCalibration(theme: BoardTheme): BoardCalibration = when (theme) {
    // Walnut master: thin rail, triangles start 0.082H; legacy tray kept for continuity.
    BoardTheme.PREMIUM_WOOD -> BoardCalibration(
        leftOrigin = 0.0770f, leftPitch = 0.0690f,
        rightOrigin = 0.5539f, rightPitch = 0.0686f,
        topBase = 0.082f, topApex = 0.422f,
        botBase = 0.908f, botApex = 0.623f,
        trayLeft = 0.940f, trayWidth = 0.048f,
        topTrayTop = 0.084f, topTrayBottom = 0.484f,
        bottomTrayTop = 0.516f, bottomTrayBottom = 0.916f
    )
    // Brushed-steel chassis: triangles hang directly under the thin top plate.
    BoardTheme.BRUSHED_METAL -> BoardCalibration(
        leftOrigin = 0.0813f, leftPitch = 0.06844f,
        rightOrigin = 0.5543f, rightPitch = 0.06776f,
        topBase = 0.155f, topApex = 0.452f,
        botBase = 0.906f, botApex = 0.610f,
        trayLeft = 0.938f, trayWidth = 0.046f,
        topTrayTop = 0.197f, topTrayBottom = 0.526f,
        bottomTrayTop = 0.563f, bottomTrayBottom = 0.848f
    )
    // Obsidian: deep ivory base band under the carved top frame.
    BoardTheme.OBSIDIAN_IVORY -> BoardCalibration(
        leftOrigin = 0.0828f, leftPitch = 0.0682f,
        rightOrigin = 0.5573f, rightPitch = 0.06715f,
        topBase = 0.154f, topApex = 0.452f,
        botBase = 0.889f, botApex = 0.621f,
        trayLeft = 0.953f, trayWidth = 0.032f,
        topTrayTop = 0.175f, topTrayBottom = 0.519f,
        bottomTrayTop = 0.570f, bottomTrayBottom = 0.840f
    )
    // Crystal ice: longest triangles, carved frost shelf above the base line.
    BoardTheme.CRYSTAL_GLASS -> BoardCalibration(
        leftOrigin = 0.0842f, leftPitch = 0.0676f,
        rightOrigin = 0.5532f, rightPitch = 0.0678f,
        topBase = 0.174f, topApex = 0.474f,
        botBase = 0.895f, botApex = 0.633f,
        trayLeft = 0.952f, trayWidth = 0.033f,
        topTrayTop = 0.190f, topTrayBottom = 0.534f,
        bottomTrayTop = 0.563f, bottomTrayBottom = 0.877f
    )
    // Royal sapphire/ruby: triangles start under the jewelled gold cornice.
    BoardTheme.ROYAL_SAPPHIRE -> BoardCalibration(
        leftOrigin = 0.0797f, leftPitch = 0.0688f,
        rightOrigin = 0.5542f, rightPitch = 0.0679f,
        topBase = 0.158f, topApex = 0.449f,
        botBase = 0.890f, botApex = 0.622f,
        trayLeft = 0.939f, trayWidth = 0.046f,
        topTrayTop = 0.205f, topTrayBottom = 0.548f,
        bottomTrayTop = 0.570f, bottomTrayBottom = 0.862f
    )
}

/**
 * Single calibrated geometry source shared by drawing and pointer input.
 * Fractions follow the per-theme masters so one physical layout drives pixels and hit tests.
 */
data class BoardGeometry private constructor(
    val width: Float,
    val height: Float,
    val calib: BoardCalibration
) {
    /** Kept for drawing code that sizes ornaments relative to the playfield. */
    val barWidth: Float get() = (calib.barRight - calib.barLeft) * width
    val playLeft: Float get() = calib.playLeft * width
    val playRight: Float get() = calib.playRight * width
    val barLeft: Float get() = calib.barLeft * width
    val columnWidth: Float get() = calib.averagePitch * width

    fun targetAt(position: Offset): Int {
        if (position.x > playRight) return Move.OFF
        if (position.x >= calib.barLeft * width && position.x <= calib.barRight * width) return Move.BAR
        val column = if (position.x < barLeft)
            ((position.x / width - calib.leftOrigin) / calib.leftPitch).roundToInt().coerceIn(0, 5)
        else 6 + ((position.x / width - calib.rightOrigin) / calib.rightPitch).roundToInt().coerceIn(0, 5)
        return if (position.y < height / 2f) 12 + column else 11 - column
    }

    /** Centre of the column that owns [point]; identical source for art, reticles and taps. */
    fun columnCenterX(point: Int): Float {
        val column = if (point < 12) 11 - point else point - 12
        val centreX = if (column < 6) calib.leftOrigin + column * calib.leftPitch
        else calib.rightOrigin + (column - 6) * calib.rightPitch
        return centreX * width
    }

    fun checkerCenter(point: Int, index: Int): Offset {
        val x = columnCenterX(point)
        val radius = columnWidth * .262f
        val stacked = index.coerceAtMost(4)
        val topRow = point >= 12
        // A five-stack fills the triangle exactly: base line to tip, with round checkers
        // at least touching. Wide canvases stretch the art sideways only, so the vertical
        // fit stays a pure height computation and no width-based rail can skew it.
        val spanPx = (if (topRow) calib.topApex - calib.topBase else calib.botBase - calib.botApex) * height
        val gap = min(radius * 1.72f, max(radius * 1.06f, (spanPx - radius * 2.05f) / 4f))
        val y = if (topRow) calib.topBase * height + radius + stacked * gap
        else calib.botBase * height - radius - stacked * gap
        return Offset(x, y)
    }

    /** Bear-off groove of the requested half, in canvas pixels. */
    fun trayRect(topHalf: Boolean): Rect =
        if (topHalf) Rect(calib.trayLeft * width, calib.topTrayTop * height, (calib.trayLeft + calib.trayWidth) * width, calib.topTrayBottom * height)
        else Rect(calib.trayLeft * width, calib.bottomTrayTop * height, (calib.trayLeft + calib.trayWidth) * width, calib.bottomTrayBottom * height)

    companion object {
        fun from(width: Float, height: Float, theme: BoardTheme = BoardTheme.PREMIUM_WOOD): BoardGeometry {
            require(width > 0f && height > 0f)
            return BoardGeometry(width, height, boardCalibration(theme))
        }
    }
}
