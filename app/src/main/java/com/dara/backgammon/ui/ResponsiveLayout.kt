package com.dara.backgammon.ui

/** Deterministic layout policy shared by phones, tablets, foldables and desktop-sized previews. */
data class BoardLayoutProfile(
    val widthFraction: Float,
    val heightFraction: Float,
    val diceOffsetDp: Int,
    val actionOffsetDp: Int,
    val compact: Boolean
)

fun boardLayoutProfile(widthDp: Float, heightDp: Float): BoardLayoutProfile {
    require(widthDp > 0 && heightDp > 0)
    val ratio = widthDp / heightDp
    return when {
        heightDp < 380f || ratio >= 2.25f -> BoardLayoutProfile(.88f, .94f, -108, 88, true)
        widthDp >= 1100f -> BoardLayoutProfile(.72f, .84f, -150, 124, false)
        widthDp >= 800f -> BoardLayoutProfile(.76f, .86f, -140, 114, false)
        else -> BoardLayoutProfile(.80f, .88f, -130, 105, false)
    }
}
