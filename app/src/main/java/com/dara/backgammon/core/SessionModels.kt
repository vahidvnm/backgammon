package com.dara.backgammon.core

enum class Mode { AI, LOCAL }

data class MatchInfo(
    val whiteScore: Int = 0,
    val blackScore: Int = 0,
    val target: Int = 5,
    val cube: Int = 1,
    val cubeOwner: Player? = null,
    val thinking: Boolean = false,
    val pendingDoubleBy: Player? = null,
    val droppedBy: Player? = null,
    val timedOutBy: Player? = null,
    val crawfordUsed: Boolean = false,
    val crawfordGame: Boolean = false
)

data class ClockState(
    val whiteMillis: Long = 60_000,
    val blackMillis: Long = 60_000,
    val turnMillis: Long = 10_000,
    val active: Boolean = true
)

data class SavedSession(
    val game: TurnState,
    val match: MatchInfo,
    val clock: ClockState,
    val mode: Mode,
    val autoAssist: Boolean
)
