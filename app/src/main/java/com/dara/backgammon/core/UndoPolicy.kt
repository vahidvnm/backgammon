package com.dara.backgammon.core

/** Defines the atomic undo boundary. Against AI one tap returns to the start of the human turn. */
object UndoPolicy {
    fun isRestoreBoundary(mode: Mode, game: TurnState): Boolean =
        mode == Mode.LOCAL || (game.position.turn == Player.WHITE && !game.rolled)
}
