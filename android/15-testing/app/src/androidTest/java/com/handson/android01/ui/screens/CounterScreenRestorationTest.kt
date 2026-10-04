package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterScreenRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // StateRestorationTester: 構成変更時の状態の保存と復元を、Activity を作り直さずに再現する
    @Test
    fun counter_keepsCountAfterRecreation() {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            HandsonAndroid01Theme {
                CounterScreen()
            }
        }
        composeTestRule.onNodeWithTag("increment_button").performClick()
        composeTestRule.onNodeWithTag("increment_button").performClick()

        // 保存された状態を使って、コンポーズの中身を作り直す（回転と同じ経路）
        tester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag("count_text").assertTextEquals("count: 2")
    }
}
