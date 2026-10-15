package com.dara.backgammon.core

data class ClockTick(
    val clock: ClockState,
    val timedOut: Boolean = false,
    val turnExpired: Boolean = false
)

/** Pure clock transition logic, separated from coroutines and Android lifecycle. */
object MatchClockEngine {
    fun tick(
        clock: ClockState,
        turn: Player,
        turnChanged: Boolean,
        shouldRun: Boolean,
        deltaMillis: Long,
        configuredTurnMillis: Long
    ): ClockTick {
        if (!clock.active || !shouldRun || deltaMillis <= 0) {
            return ClockTick(if (turnChanged) clock.copy(turnMillis = configuredTurnMillis) else clock)
        }
        val base = if (turnChanged) clock.copy(turnMillis = configuredTurnMillis) else clock
        val bank = (if (turn == Player.WHITE) base.whiteMillis else base.blackMillis) - deltaMillis
        val turnLeft = base.turnMillis - deltaMillis
        val next = if (turn == Player.WHITE) {
            base.copy(whiteMillis = bank.coerceAtLeast(0), turnMillis = turnLeft.coerceAtLeast(0))
        } else {
            base.copy(blackMillis = bank.coerceAtLeast(0), turnMillis = turnLeft.coerceAtLeast(0))
        }
        return ClockTick(
            clock = next,
            timedOut = bank <= 0,
            turnExpired = bank > 0 && turnLeft <= 0
        )
    }
}
