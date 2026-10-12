package com.dara.backgammon

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.Lifecycle
import com.dara.backgammon.core.Player
import com.dara.backgammon.core.Position
import com.dara.backgammon.core.TurnState
import com.dara.backgammon.data.BoardTheme
import com.dara.backgammon.data.PieceStyle
import com.dara.backgammon.data.DiceStyle
import com.dara.backgammon.data.Preferences
import com.dara.backgammon.data.TableScene
import com.dara.backgammon.ui.AnimatedDice
import com.dara.backgammon.ui.FlatBoard
import com.dara.backgammon.ui.VictoryCelebration
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgammonJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun startLocal() {
        compose.onNodeWithText("بازی دونفره محلی").performClick()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
    }

    private fun rollAndSettle() {
        compose.onNode(hasContentDescription("تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید", substring = true)).performClick()
        compose.mainClock.advanceTimeBy(2_200)
        Thread.sleep(2_000)
        compose.waitForIdle()
    }

    @Test fun localGameRollsAndSurvivesActivityRecreation() {
        startLocal()
        rollAndSettle()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
    }

    @Test fun undoCanBeUsedRepeatedlyWithoutLosingTheGame() {
        startLocal()
        repeat(5) {
            rollAndSettle()
            compose.onNodeWithContentDescription("بازگردانی").performClick()
            compose.waitForIdle()
            compose.onNode(hasContentDescription("تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید", substring = true)).assertExists()
            compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        }
    }

    @Test fun aiJourneySupportsRepeatedUndo() {
        compose.onNodeWithText("بازی با هوش مصنوعی").performClick()
        compose.onNodeWithText("بازی با هوش مصنوعی").performClick()
        compose.onNodeWithText("تازه‌کار").performClick()
        repeat(3) {
            rollAndSettle()
            compose.onNodeWithContentDescription("بازگردانی").performClick()
            compose.waitForIdle()
            compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        }
    }

    @Test fun clockSettingsBackgroundAndForegroundRemainUsable() {
        compose.onNode(hasText("شروع بازی") or hasText("بازی جدید")).performClick()
        compose.onNodeWithText("گزینه‌ها").performClick()
        compose.onNodeWithText("سریع — ۱ دقیقه ذخیره / ۱۰ ثانیه هر نوبت").performClick()
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("بازی دونفره محلی").performClick()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(800)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
    }

    @Test fun newInstallDefaultsToPersianRug() {
        compose.activity.getSharedPreferences("settings", 0).edit().clear().commit()
        assertEquals(TableScene.PERSIAN_RUG, Preferences(compose.activity).load().tableScene)
    }

    @Test fun firstThrowHintIsExplicitAndCanBeHiddenForever() {
        compose.activity.setContent { AnimatedDice(emptyList(), DiceStyle.CLASSIC, false, true, {}, Modifier.fillMaxSize()) }
        compose.onNode(hasContentDescription("راهنمای پیکان اولین پرتاب", substring = true)).assertExists()
        compose.activity.setContent { AnimatedDice(emptyList(), DiceStyle.CLASSIC, false, false, {}, Modifier.fillMaxSize()) }
        compose.onNode(hasContentDescription("راهنمای پیکان اولین پرتاب", substring = true)).assertDoesNotExist()
    }

    @Test fun borneOffCheckerAnimatesIntoTheStorageTray() {
        lateinit var state: MutableState<TurnState>
        compose.activity.setContent {
            state = remember { mutableStateOf(TurnState(Position(IntArray(24).also { it[0] = 1 }, offWhite = 14, offBlack = 15, turn = Player.WHITE))) }
            FlatBoard(state.value, BoardTheme.PREMIUM_WOOD, PieceStyle.KHATAM, null, emptyList(), {}, {}, modifier = Modifier.fillMaxSize())
        }
        compose.waitForIdle()
        compose.activity.runOnUiThread { state.value = state.value.copy(position = Position(IntArray(24), offWhite = 15, offBlack = 15, turn = Player.WHITE)) }
        compose.mainClock.advanceTimeBy(1_100)
        compose.waitForIdle()
        compose.onNode(hasContentDescription("سفید 15 مهره خارج", substring = true)).assertExists()
    }

    @Test fun victoryCelebrationRendersAnimatedScoreboard() {
        compose.activity.setContent { VictoryCelebration(Player.WHITE, 5, 3, Modifier.fillMaxSize()) }
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        compose.onNode(hasContentDescription("تابلوی پایان بازی", substring = true)).assertExists()
        compose.onNodeWithText("5").assertExists()
        compose.onNodeWithText("3").assertExists()
    }

    @Test fun setupAndSettingsSectionsRemainReachable() {
        compose.onNode(hasText("شروع بازی") or hasText("بازی جدید")).performClick()
        compose.onNodeWithText("بازی با هوش مصنوعی").assertExists()
        compose.onNodeWithText("گزینه‌ها").performClick()
        listOf("بازی", "صدا", "ظاهر", "راهنما", "درباره").forEach { section ->
            compose.onNodeWithText(section).performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("بازگشت  ›").assertExists()
    }
}
