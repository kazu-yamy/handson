package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 削除 → Snackbar → 元に戻す、の流れを画面全体（TodoListPage）でテストする
@RunWith(AndroidJUnit4::class)
class TodoSnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPage() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListPage(initialTodos = demoTodoList)
            }
        }
    }

    private fun deleteFirstRow() {
        composeTestRule.onAllNodesWithContentDescription("削除")[0].performClick()
    }

    @Test
    fun deleting_removesRowAndShowsSnackbar() {
        setPage()

        deleteFirstRow()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(2)
        composeTestRule.onNodeWithText("「learn kotlin basics」を削除しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("元に戻す").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 件中 1 件完了").assertIsDisplayed()
    }

    @Test
    fun undo_restoresRowAtOriginalPosition() {
        setPage()
        deleteFirstRow()

        composeTestRule.onNodeWithText("元に戻す").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
        // 先頭に戻っている（末尾に追加されていない）
        composeTestRule.onAllNodesWithTag("todo_row")[0].assert(hasAnyDescendant(hasText("Learn Kotlin Basics")))
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun fab_addsTodo() {
        setPage()

        composeTestRule.onNodeWithTag("fab_add").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(4)
    }
}
