package com.dara.backgammon.ai

import com.dara.backgammon.core.GameEngine
import com.dara.backgammon.core.Player
import com.dara.backgammon.core.TurnState
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strength regression lock for the five-level ladder.
 *
 * Playouts share one seeded dice stream, so the table is deterministic for a given build. The
 * test locks the property that matters most for a player: no level is so lopsided that the
 * weaker side is hopeless, and the stronger level keeps a clear majority. Forty games per
 * pairing, weaker level on White and stronger level on Black, measured on this revision as:
 *
 *   BEGINNER vs EASY   -> 14 : 26   (before the ladder, BEGINNER lost 198 of 200 games)
 *   EASY     vs MEDIUM -> 12 : 28
 *   MEDIUM   vs HARD   -> 15 : 25   (before the ladder, MEDIUM/HARD were only 85 : 115 apart)
 *
 * The thresholds below are deliberately one notch looser than the measured table. The exact
 * strength table is re-measured over 200 games per pairing during release validation, as
 * documented in the release checklist; the deterministic window guarantees live in
 * [DifficultyPolicyTest].
 *
 * EXPERT is excluded on purpose: its reply search costs roughly 2.5 seconds per game, which
 * would dominate the unit-test budget. Its measured edge over HARD is checked during release
 * validation instead.
 */
class DifficultyLadderTest {
    private fun playPairing(weaker: Difficulty, stronger: Difficulty, games: Int = 40): Pair<Int, Int> {
        val random = Random(31337)
        var weakerWins = 0
        var strongerWins = 0
        repeat(games) {
            var state = TurnState()
            var turns = 0
            while (state.winner == null && turns < 900) {
                if (!state.rolled) {
                    state = state.copy(
                        dice = GameEngine.rollValues(random.nextInt(1, 7), random.nextInt(1, 7)),
                        rolled = true
                    )
                }
                val level = if (state.position.turn == Player.WHITE) weaker else stronger
                val sequence = BackgammonAi.chooseSequence(state.position, state.dice, level, random = random)
                if (sequence.isEmpty()) {
                    state = GameEngine.endTurn(state)
                    turns++
                    continue
                }
                sequence.forEach { move -> state = GameEngine.afterMove(state, move) }
                if (state.winner == null) state = GameEngine.endTurn(state)
                turns++
            }
            when (state.winner) {
                Player.BLACK -> strongerWins++
                Player.WHITE -> weakerWins++
                null -> Unit
            }
        }
        return weakerWins to strongerWins
    }

    @Test fun everyLevelClearlyBeatsTheOneBelowItAndStaysBeatable() {
        listOf(
            Difficulty.BEGINNER to Difficulty.EASY,
            Difficulty.EASY to Difficulty.MEDIUM,
            Difficulty.MEDIUM to Difficulty.HARD
        ).forEach { (weaker, stronger) ->
            val (weakerWins, strongerWins) = playPairing(weaker, stronger)
            assertTrue(
                "$stronger must clearly outplay $weaker, measured $weakerWins : $strongerWins",
                strongerWins >= 22
            )
            assertTrue(
                "$stronger must remain beatable by $weaker, measured $weakerWins : $strongerWins",
                weakerWins >= 4
            )
        }
    }
}
