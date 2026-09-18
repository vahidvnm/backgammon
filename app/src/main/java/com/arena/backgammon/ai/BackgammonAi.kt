package com.arena.backgammon.ai

import com.arena.backgammon.core.*
import kotlin.math.abs
import kotlin.random.Random

enum class Difficulty { BEGINNER, EASY, MEDIUM, HARD, EXPERT }

object BackgammonAi {
    fun chooseSequence(position: Position, dice: List<Int>, level: Difficulty, random: Random = Random.Default): List<Move> {
        val choices = GameEngine.sequences(position, dice).filter { it.isNotEmpty() }
        if (choices.isEmpty()) return emptyList()
        if (level == Difficulty.BEGINNER) return choices.random(random)
        val scored = choices.map { seq -> seq to evaluate(play(position, seq), position.turn) }
        val sorted = scored.sortedByDescending { it.second }
        return when (level) {
            Difficulty.EASY -> sorted.take(minOf(4, sorted.size)).random(random).first
            Difficulty.MEDIUM -> sorted.take(minOf(2, sorted.size)).random(random).first
            Difficulty.HARD -> sorted.first().first
            Difficulty.EXPERT -> sorted.maxBy { (seq, score) -> score + replySafety(play(position,seq), position.turn) }.first
            else -> choices.first()
        }
    }
    private fun play(start: Position, moves: List<Move>) = moves.fold(start) { p,m -> GameEngine.apply(p,m) }
    private fun evaluate(p: Position, me: Player): Double {
        fun pip(player: Player): Int = (0..23).sumOf { i ->
            val count = (p.points[i] * player.sign).coerceAtLeast(0)
            count * if (player == Player.WHITE) i + 1 else 24 - i
        } + p.bar(player) * 25
        var score = (pip(me.other()) - pip(me)) * 1.2 + (p.off(me)-p.off(me.other()))*45
        for (i in 0..23) {
            val mine=p.points[i]*me.sign
            if (mine >= 2) score += 3.5 + minOf(mine,3)
            if (mine == 1) score -= 2.8
            if (p.points[i]*me.other().sign == 1 && abs(i-12)<10) score += .25
        }
        score += p.bar(me.other())*18 - p.bar(me)*22
        return score
    }
    private fun replySafety(p: Position, me: Player): Double {
        val opponent = p.copy(turn=me.other())
        var danger=0.0
        // Expert estimates exposure over all 21 possible rolls without expensive deep search.
        for (a in 1..6) for (b in a..6) {
            val sequences=GameEngine.sequences(opponent,GameEngine.rollValues(a,b))
            val worst=sequences.minOfOrNull { evaluate(play(opponent,it),me) } ?: evaluate(p,me)
            danger += worst * if (a==b) 1.0 else 2.0
        }
        return danger/36.0 * .22
    }
}
