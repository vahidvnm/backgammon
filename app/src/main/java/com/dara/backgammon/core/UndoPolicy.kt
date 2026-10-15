package com.dara.backgammon.core

/** Defines undo boundaries. AI undo replays the human turn with the original dice. */
object UndoPolicy {
    fun isHumanRolledState(game: TurnState): Boolean =
        game.position.turn == Player.WHITE && game.rolled && game.dice.isNotEmpty()
}
