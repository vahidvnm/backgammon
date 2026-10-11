package com.dara.backgammon.audio

import android.content.Context
import android.media.SoundPool
import com.dara.backgammon.R
import com.dara.backgammon.data.BoardTheme

/** Owns short natural game sounds and their lifecycle. No synthetic tones are generated. */
class GameAudio(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
    private val diceSound = soundPool.load(context, R.raw.dice_roll, 1)
    private val checkerSound = soundPool.load(context, R.raw.checker_place, 1)

    fun playDice(enabled: Boolean) {
        if (enabled) soundPool.play(diceSound, .76f, .76f, 1, 0, 1f)
    }

    fun playChecker(enabled: Boolean, theme: BoardTheme) {
        if (!enabled) return
        val rate = when (theme) {
            BoardTheme.PREMIUM_WOOD -> .92f
            BoardTheme.BRUSHED_METAL -> 1.08f
            BoardTheme.OBSIDIAN_IVORY -> .82f
            BoardTheme.CRYSTAL_GLASS -> 1.16f
            BoardTheme.ROYAL_SAPPHIRE -> 1f
        }
        soundPool.play(checkerSound, .48f, .48f, 1, 0, rate)
    }

    fun release() = soundPool.release()
}
