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
        composeTestRule.onNodeWithText("「build compose screen」を削除しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("元に戻す").assertIsDisplayed()
        // 削除したのは未完了の行なので、完了数は 2 のまま
        composeTestRule.onNodeWithText("2 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun undo_restoresRowAtOriginalPosition() {
        setPage()
        deleteFirstRow()

        composeTestRule.onNodeWithText("元に戻す").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
        // 一覧の先頭（未完了のセクションの先頭）に戻っている
        composeTestRule.onAllNodesWithTag("todo_row")[0].assert(hasAnyDescendant(hasText("Build Compose Screen")))
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun fab_opensAddSheet() {
        setPage()
        composeTestRule.onNodeWithTag("add_sheet").assertDoesNotExist()

        composeTestRule.onNodeWithTag("fab_add").performClick()

        composeTestRule.onNodeWithTag("add_sheet").assertIsDisplayed()
    }
}
