package com.dara.backgammon.core

/** Pure validation used before accepting a restored process-death snapshot. */
object SavedSessionValidator {
    private val validCubes = setOf(1, 2, 4, 8, 16, 32, 64)

    fun isValid(session: SavedSession): Boolean {
        val game = session.game
        val match = session.match
        val clock = session.clock
        return validPosition(game.position) &&
            game.dice.size <= 4 && game.dice.all { it in 1..6 } &&
            (game.rolled || game.dice.isEmpty()) &&
            match.whiteScore >= 0 && match.blackScore >= 0 &&
            match.target in 1..25 && match.cube in validCubes &&
            !(match.crawfordGame && !match.crawfordUsed) &&
            clock.whiteMillis >= 0 && clock.blackMillis >= 0 && clock.turnMillis >= 0
    }

    fun validPosition(position: Position): Boolean {
        if (position.points.size != 24 || position.points.any { it !in -15..15 }) return false
        if (listOf(position.barWhite, position.barBlack, position.offWhite, position.offBlack).any { it !in 0..15 }) return false
        val white = position.points.sumOf { it.coerceAtLeast(0) } + position.barWhite + position.offWhite
        val black = position.points.sumOf { (-it).coerceAtLeast(0) } + position.barBlack + position.offBlack
        return white == 15 && black == 15
    }
}
