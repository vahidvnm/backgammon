package com.dara.backgammon.core

/** Decides when challenge time is charged without depending on Android/coroutines. */
object ClockRunPolicy {
    fun shouldRun(
        mode: Mode,
        turn: Player,
        waitingForHumanInput: Boolean,
        gameOver: Boolean,
        doubleOfferPending: Boolean
    ): Boolean {
        if (gameOver || doubleOfferPending) return false
        // In an AI challenge its whole turn is timed, including thinking, rolling and animation.
        // Otherwise players would see the opponent's clock remain frozen.
        return (mode == Mode.AI && turn == Player.BLACK) || waitingForHumanInput
    }
}
