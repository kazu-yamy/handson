package com.handson.android01.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.screens.testViewModel
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 画面遷移（一覧 ⇄ 詳細）のテスト。TodoApp 全体を、デモデータの ViewModel で動かす
@RunWith(AndroidJUnit4::class)
class TodoNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoApp(viewModel = testViewModel())
            }
        }
    }

    // 一覧は「未完了」「完了」の順なので、先頭の行は未完了の "build compose screen"
    private fun openFirstTodo() {
        composeTestRule.onAllNodesWithTag("todo_title")[0].performClick()
    }

    @Test
    fun tappingTitle_opensDetailOfThatTodo() {
        openFirstTodo()

        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Build Compose Screen")
        composeTestRule.onNodeWithTag("detail_status").assertTextEquals("未完了")
        // 一覧は画面から外れている
        composeTestRule.onNodeWithTag("todo_list").assertDoesNotExist()
    }

    @Test
    fun appBarBackButton_returnsToList() {
        openFirstTodo()

        composeTestRule.onNodeWithTag("detail_back").performClick()

        composeTestRule.onNodeWithTag("detail_title").assertDoesNotExist()
        composeTestRule.onNodeWithTag("todo_list").assertIsDisplayed()
    }

    @Test
    fun systemBack_returnsToList() {
        openFirstTodo()

        // システムの戻る操作（戻るボタン・ジェスチャー）。NavDisplay の onBack が呼ばれる
        Espresso.pressBack()

        composeTestRule.onNodeWithTag("detail_title").assertDoesNotExist()
        composeTestRule.onNodeWithTag("todo_list").assertIsDisplayed()
    }

    @Test
    fun togglingInDetail_isReflectedInList() {
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
        openFirstTodo()

        composeTestRule.onNodeWithTag("detail_checkbox").performClick()
        composeTestRule.onNodeWithTag("detail_status").assertTextEquals("完了")
        composeTestRule.onNodeWithTag("detail_back").performClick()

        composeTestRule.onNodeWithText("3 件中 3 件完了").assertIsDisplayed()
    }

    @Test
    fun deletingInDetail_returnsToListWithoutThatRow() {
        openFirstTodo()

        composeTestRule.onNodeWithTag("detail_delete").performClick()

        composeTestRule.onNodeWithTag("detail_title").assertDoesNotExist()
        composeTestRule.onNodeWithText("2 件中 2 件完了").assertIsDisplayed()
        composeTestRule.onNodeWithText("Build Compose Screen").assertDoesNotExist()
    }

    @Test
    fun deletingInDetail_neverShowsNotFoundDuringTransition() {
        openFirstTodo()
        // 詳細への遷移アニメーションが終わってから止める
        composeTestRule.onNodeWithTag("detail_delete").assertIsDisplayed()

        // アニメーションを止め、削除して戻る途中の状態で確かめる
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("detail_delete").performClick()
        repeat(10) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("detail_not_found").assertDoesNotExist()
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.onNodeWithTag("detail_title").assertDoesNotExist()
    }
}
