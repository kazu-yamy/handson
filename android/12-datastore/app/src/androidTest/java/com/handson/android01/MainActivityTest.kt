package com.handson.android01

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.screens.FakeTodoApiRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    // createAndroidComposeRule<MainActivity>(): 実際の Activity を起動してテストする。
    // createComposeRule() と違い、setContent は使わず、アプリ本来の画面をそのまま確かめる
    // アプリが使う TodoApi を、起動の前に偽のサーバーへ差し替える（ネットワークに出ない）
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FakeTodoApiRule()).around(composeTestRule)

    @Test
    fun launchedActivity_showsTodoListScreen() {
        composeTestRule.onNodeWithText("Todo").assertIsDisplayed()
    }
}
