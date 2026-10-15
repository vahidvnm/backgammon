package com.dara.backgammon.data

import android.content.Context
import com.dara.backgammon.core.ClockState
import com.dara.backgammon.core.MatchInfo
import com.dara.backgammon.core.Mode
import com.dara.backgammon.core.Player
import com.dara.backgammon.core.Position
import com.dara.backgammon.core.SavedSession
import com.dara.backgammon.core.SavedSessionValidator
import com.dara.backgammon.core.TurnState
import org.json.JSONObject

/** Versioned, validated local persistence for an offline match. */
class SavedMatchStore(context: Context) {
    private val preferences = context.getSharedPreferences("saved_match", Context.MODE_PRIVATE)

    fun save(session: SavedSession): Boolean = runCatching {
        val game = session.game
        val match = session.match
        val position = game.position
        val clock = session.clock
        val json = JSONObject().apply {
            put("schema", SCHEMA)
            put("points", position.points.joinToString(","))
            put("bw", position.barWhite); put("bb", position.barBlack)
            put("ow", position.offWhite); put("ob", position.offBlack)
            put("turn", position.turn.name)
            put("dice", game.dice.joinToString(",")); put("rolled", game.rolled)
            put("winner", game.winner?.name ?: "")
            put("ws", match.whiteScore); put("bs", match.blackScore); put("target", match.target)
            put("cube", match.cube); put("owner", match.cubeOwner?.name ?: "")
            put("pending", match.pendingDoubleBy?.name ?: "")
            put("dropped", match.droppedBy?.name ?: ""); put("timed", match.timedOutBy?.name ?: "")
            put("crawfordUsed", match.crawfordUsed); put("crawfordGame", match.crawfordGame)
            put("mode", session.mode.name); put("auto", session.autoAssist)
            put("clockWhite", clock.whiteMillis); put("clockBlack", clock.blackMillis)
            put("clockTurn", clock.turnMillis); put("clockActive", clock.active)
        }
        check(preferences.edit().putString(KEY_STATE, json.toString()).commit())
    }.isSuccess

    fun load(fallbackClock: ClockState): SavedSession? {
        val raw = preferences.getString(KEY_STATE, null) ?: return null
        return runCatching { decode(raw, fallbackClock) }.getOrElse {
            clear()
            null
        }
    }

    fun clear() = preferences.edit().remove(KEY_STATE).apply()

    private fun decode(raw: String, fallbackClock: ClockState): SavedSession {
        val json = JSONObject(raw)
        fun player(key: String): Player? = json.optString(key)
            .takeIf(String::isNotEmpty)?.let(Player::valueOf)

        val points = json.getString("points").split(',').map(String::toInt).toIntArray()
        require(points.size == 24)
        val position = Position(
            points, json.getInt("bw"), json.getInt("bb"), json.getInt("ow"),
            json.getInt("ob"), Player.valueOf(json.getString("turn"))
        )

        val dice = json.optString("dice").takeIf(String::isNotEmpty)
            ?.split(',')?.map(String::toInt) ?: emptyList()
        require(dice.size <= 4 && dice.all { it in 1..6 })
        val game = TurnState(position, dice, json.optBoolean("rolled"), player("winner"))
        require(game.rolled || dice.isEmpty())

        val match = MatchInfo(
            whiteScore = json.optInt("ws"), blackScore = json.optInt("bs"),
            target = json.optInt("target", 5), cube = json.optInt("cube", 1),
            cubeOwner = player("owner"), pendingDoubleBy = player("pending"),
            droppedBy = player("dropped"), timedOutBy = player("timed"),
            crawfordUsed = json.optBoolean("crawfordUsed"),
            crawfordGame = json.optBoolean("crawfordGame")
        )

        val clock = if (json.optInt("schema", 1) >= 2) ClockState(
            json.optLong("clockWhite", fallbackClock.whiteMillis),
            json.optLong("clockBlack", fallbackClock.blackMillis),
            json.optLong("clockTurn", fallbackClock.turnMillis),
            json.optBoolean("clockActive", fallbackClock.active)
        ) else fallbackClock

        val mode = runCatching { Mode.valueOf(json.optString("mode")) }.getOrDefault(Mode.AI)
        val session=SavedSession(game, match, clock, mode, json.optBoolean("auto"))
        require(SavedSessionValidator.isValid(session))
        return session
    }

    private companion object {
        const val KEY_STATE = "state"
        const val SCHEMA = 2
    }
}
