package com.dara.backgammon

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        compose.onNodeWithContentDescription("تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید").performClick()
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
            compose.onNodeWithContentDescription("تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید").assertExists()
            compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        }
    }

    @Test fun aiAndAutoJourneyKeepsProgressing() {
        compose.onNodeWithText("بازی با هوش مصنوعی").performClick()
        compose.onNodeWithText("بازی با هوش مصنوعی").performClick()
        compose.onNodeWithText("تازه‌کار").performClick()
        rollAndSettle()
        compose.onNodeWithContentDescription("خودکار خاموش").performClick()
        compose.mainClock.advanceTimeBy(8_000)
        Thread.sleep(4_000)
        compose.waitForIdle()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
        compose.onNodeWithContentDescription("بازگردانی").performClick()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
    }

    @Test fun clockSettingsBackgroundAndForegroundRemainUsable() {
        compose.onNode(hasText("شروع بازی") or hasText("بازی جدید")).performClick()
        compose.onNodeWithText("گزینه‌ها").performClick()
        compose.onNodeWithText("سریع — ۱ دقیقه ذخیره / ۱۰ ثانیه هر نوبت").performClick()
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("بازی دونفره محلی").performClick()
        compose.onNode(hasContentDescription("زمان بازیکن سفید", substring = true)).assertExists()
        compose.onNode(hasContentDescription("زمان بازیکن سیاه", substring = true)).assertExists()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(800)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNode(hasContentDescription("زمان بازیکن سفید", substring = true)).assertExists()
        compose.onNode(hasContentDescription("تخته نرد", substring = true)).assertExists()
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
