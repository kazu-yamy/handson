package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 行を左へスワイプして削除する（SwipeToDismissBox）
@RunWith(AndroidJUnit4::class)
class TodoSwipeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPage() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListPage(initialTodos = demoTodoList)
            }
        }
    }

    @Test
    fun swipeLeft_deletesRowAndShowsSnackbar() {
        setPage()

        composeTestRule.onAllNodesWithTag("todo_row")[0].performTouchInput { swipeLeft() }

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(2)
        composeTestRule.onNodeWithText("「build compose screen」を削除しました").assertIsDisplayed()
        // 次の行が（スワイプの状態を引き継がず）普通に見えている
        composeTestRule.onNodeWithText("Learn Kotlin Basics").assertIsDisplayed()
    }

    @Test
    fun swipeRight_doesNotDelete() {
        setPage()

        composeTestRule.onAllNodesWithTag("todo_row")[0].performTouchInput { swipeRight() }

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun swipeLeft_thenUndo_restoresRow() {
        setPage()
        composeTestRule.onAllNodesWithTag("todo_row")[0].performTouchInput { swipeLeft() }

        composeTestRule.onNodeWithText("元に戻す").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
        // 戻った行は、スワイプで消えた状態ではなく、普通に見えている
        composeTestRule.onNodeWithText("Build Compose Screen").assertIsDisplayed()
    }
}
