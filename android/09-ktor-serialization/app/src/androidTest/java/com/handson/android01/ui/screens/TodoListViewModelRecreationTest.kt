package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.MainActivity
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

// Activity を実際に作り直して（画面回転と同じ）、ViewModel の状態が残ることを確かめる。
// StateRestorationTester は Activity を作り直さないので、ViewModel のテストには向かない
@RunWith(AndroidJUnit4::class)
class TodoListViewModelRecreationTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    // アプリが使う TodoApi を、起動の前に偽のサーバーへ差し替える（ネットワークに出ない）
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FakeTodoApiRule()).around(composeTestRule)

    @Test
    fun toggles_surviveActivityRecreation() {
        // 起動直後は読み込み中（偽のサーバーの応答を待つ）。応答は別のスレッドから届くので、一覧が出るまで waitUntil で待つ
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("todo_row").fetchSemanticsNodes().isNotEmpty()
        }
        // 読み込み後は 20 件中 7 件が完了。先頭の未完了の行を完了にする
        composeTestRule.onNodeWithText("20 件中 7 件完了").assertIsDisplayed()
        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.onNodeWithText("20 件中 8 件完了").assertIsDisplayed()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("20 件中 8 件完了").assertIsDisplayed()
    }

    @Test
    fun filter_survivesActivityRecreation() {
        // 読み込み中はフィルタが出ないので、一覧が出るまで待つ
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("todo_row").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("filter_Done").performClick()
        composeTestRule.onNodeWithTag("filter_Done").assertIsSelected()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("filter_Done").assertIsSelected()
    }
}
