package com.handson.android01

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    // createAndroidComposeRule<MainActivity>(): 実際の Activity を起動してテストする。
    // createComposeRule() と違い、setContent は使わず、アプリ本来の画面をそのまま確かめる
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    // HiltAndroidRule は一番外側に置く（Activity の起動より前に、Hilt のコンポーネントを作るため）。偽物への差し替えは FakeDataModule が行う（全テストに効く）
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(HiltAndroidRule(this)).around(composeTestRule)

    @Test
    fun launchedActivity_showsTodoListScreen() {
        composeTestRule.onNodeWithText("Todo").assertIsDisplayed()
    }
}
