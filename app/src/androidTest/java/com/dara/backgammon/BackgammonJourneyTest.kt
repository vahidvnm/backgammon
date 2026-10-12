package com.dara.backgammon

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgammonJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchLocalGameRollDiceAndSurviveRecreation() {
        compose.onNodeWithText("بازی دونفره محلی").assertExists().performClick()
        compose.onNode(hasContentDescription("تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید"))
            .assertExists().performClick()
        compose.waitUntil(timeoutMillis = 8_000) {
            compose.onAllNodes(hasContentDescription("نتیجه تاس", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNode(hasContentDescription("نتیجه تاس", substring = true)).assertExists()
    }

    @Test fun setupAndSettingsRemainReachable() {
        compose.onNode(hasText("شروع بازی") or hasText("بازی جدید")).performClick()
        compose.onNodeWithText("بازی با هوش مصنوعی").assertExists()
        compose.onNodeWithText("گزینه‌ها").assertExists().performClick()
        compose.onNodeWithText("شیوه بازی").assertExists()
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("بازگشت  ›").assertExists()
    }
}
