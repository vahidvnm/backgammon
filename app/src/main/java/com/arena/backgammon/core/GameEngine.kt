package com.arena.backgammon.core

enum class Player(val sign: Int) { WHITE(1), BLACK(-1); fun other() = if (this == WHITE) BLACK else WHITE }
data class Move(val from: Int, val to: Int, val die: Int) { companion object { const val BAR = 24; const val OFF = 25 } }
data class Position(
    val points: IntArray = initialPoints(),
    val barWhite: Int = 0, val barBlack: Int = 0,
    val offWhite: Int = 0, val offBlack: Int = 0,
    val turn: Player = Player.WHITE
) {
    fun bar(p: Player) = if (p == Player.WHITE) barWhite else barBlack
    fun off(p: Player) = if (p == Player.WHITE) offWhite else offBlack
    fun copyDeep() = copy(points = points.copyOf())
    companion object {
        fun initialPoints() = IntArray(24).apply {
            this[23]=2; this[12]=5; this[7]=3; this[5]=5
            this[0]=-2; this[11]=-5; this[16]=-3; this[18]=-5
        }
    }
}

data class TurnState(val position: Position = Position(), val dice: List<Int> = emptyList(), val rolled: Boolean = false, val winner: Player? = null)

/** Pure, UI-independent standard backgammon rules. Points run from White's home (0) to Black's home (23). */
object GameEngine {
    fun rollValues(a: Int, b: Int) = if (a == b) List(4) { a } else listOf(a, b)

    fun rawMoves(pos: Position, die: Int): List<Move> {
        val p = pos.turn
        if (pos.bar(p) > 0) {
            val destination = if (p == Player.WHITE) 24 - die else die - 1
            return if (open(pos, destination, p)) listOf(Move(Move.BAR, destination, die)) else emptyList()
        }
        val result = mutableListOf<Move>()
        for (from in 0..23) if (pos.points[from] * p.sign > 0) {
            val to = from + if (p == Player.WHITE) -die else die
            if (to in 0..23 && open(pos, to, p)) result += Move(from, to, die)
            else if (to !in 0..23 && canBearOff(pos, p, from, die)) result += Move(from, Move.OFF, die)
        }
        return result
    }

    private fun open(pos: Position, point: Int, p: Player) = pos.points[point] * p.sign >= -1
    private fun canBearOff(pos: Position, p: Player, from: Int, die: Int): Boolean {
        if (pos.bar(p) > 0) return false
        val home = if (p == Player.WHITE) 0..5 else 18..23
        if ((0..23).any { it !in home && pos.points[it] * p.sign > 0 }) return false
        val exact = if (p == Player.WHITE) from + 1 == die else 24 - from == die
        if (exact) return true
        return if (p == Player.WHITE && die > from + 1) (from + 1..5).none { pos.points[it] > 0 }
        else if (p == Player.BLACK && die > 24 - from) (18 until from).none { pos.points[it] < 0 }
        else false
    }

    fun apply(pos: Position, move: Move): Position {
        require(move in rawMoves(pos, move.die)) { "Illegal move: $move" }
        val board = pos.points.copyOf(); val p = pos.turn; var bw=pos.barWhite; var bb=pos.barBlack; var ow=pos.offWhite; var ob=pos.offBlack
        if (move.from == Move.BAR) { if (p == Player.WHITE) bw-- else bb-- } else board[move.from] -= p.sign
        if (move.to == Move.OFF) { if (p == Player.WHITE) ow++ else ob++ }
        else {
            if (board[move.to] == -p.sign) { board[move.to] = 0; if (p == Player.WHITE) bb++ else bw++ }
            board[move.to] += p.sign
        }
        return Position(board,bw,bb,ow,ob,p)
    }

    /** Complete legal continuations. Filtering by longest sequence enforces maximum dice use and the higher-die rule. */
    fun sequences(pos: Position, dice: List<Int>): List<List<Move>> {
        if (dice.isEmpty()) return listOf(emptyList())
        val all = mutableListOf<List<Move>>()
        dice.distinct().forEach { die ->
            val rest = dice.toMutableList().also { it.remove(die) }
            rawMoves(pos, die).forEach { move -> sequences(apply(pos, move), rest).forEach { all += listOf(move) + it } }
        }
        if (all.isEmpty()) return listOf(emptyList())
        val max = all.maxOf { it.size }
        var best = all.filter { it.size == max }.distinct()
        if (max == 1 && dice.distinct().size > 1) {
            val high = best.maxOf { it.first().die }; best = best.filter { it.first().die == high }
        }
        return best
    }

    fun legalMoves(pos: Position, dice: List<Int>) = sequences(pos, dice).mapNotNull { it.firstOrNull() }.distinct()
    fun afterMove(state: TurnState, move: Move): TurnState {
        require(move in legalMoves(state.position, state.dice))
        val nextPos = apply(state.position, move)
        val remaining = state.dice.toMutableList().also { it.remove(move.die) }
        val won = if (nextPos.off(nextPos.turn) == 15) nextPos.turn else null
        return TurnState(nextPos, remaining, true, won)
    }
    fun endTurn(state: TurnState): TurnState {
        require(state.rolled && (state.dice.isEmpty() || legalMoves(state.position,state.dice).isEmpty()))
        return state.copy(position=state.position.copy(turn=state.position.turn.other()),dice=emptyList(),rolled=false)
    }
}
