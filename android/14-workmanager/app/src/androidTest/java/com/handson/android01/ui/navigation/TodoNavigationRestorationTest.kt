package com.handson.android01.ui.navigation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

// 実際の Activity を作り直しても（画面回転と同じ）、開いている詳細画面が残ることを確かめる
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodoNavigationRestorationTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    // HiltAndroidRule は一番外側に置く（Activity の起動より前に、Hilt のコンポーネントを作るため）。偽物への差し替えは FakeDataModule が行う（全テストに効く）
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(HiltAndroidRule(this)).around(composeTestRule)

    @Test
    fun detailScreen_survivesActivityRecreation() {
        // 一覧は ViewModel が偽のサーバーから取得してから出る。行が出るのを待つ
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("todo_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag("todo_title")[0].performClick()
        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Task 2")

        composeTestRule.activityRule.scenario.recreate()

        // 作り直したあとも、バックスタックが戻って詳細が表示される
        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Task 2")
    }
}
