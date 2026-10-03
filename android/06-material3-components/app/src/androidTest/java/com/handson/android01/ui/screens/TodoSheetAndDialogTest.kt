package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 追加用のボトムシート（ModalBottomSheet）と、完了済みを削除する確認ダイアログ（AlertDialog）
@RunWith(AndroidJUnit4::class)
class TodoSheetAndDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPage(initial: List<Todo> = demoTodoList) {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListPage(initialTodos = initial)
            }
        }
    }

    @Test
    fun addSheet_isClosedAtFirst() {
        setPage()

        composeTestRule.onNodeWithTag("add_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("todo_input").assertDoesNotExist()
    }

    @Test
    fun addSheet_addsTodoAndCloses() {
        setPage()
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")

        composeTestRule.onNodeWithTag("add_button").performClick()

        // hide() のアニメーションが終わったあとに、表示フラグが落ちてシートが消える
        composeTestRule.onNodeWithTag("add_sheet").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(4)
        composeTestRule.onNodeWithText("Buy Milk").assertIsDisplayed()
    }

    @Test
    fun addSheet_closesWhenSwipedDown() {
        setPage()
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("add_sheet").assertIsDisplayed()

        // 下へドラッグして閉じる（onDismissRequest が呼ばれる）
        composeTestRule.onNodeWithTag("add_sheet").performTouchInput { swipeDown() }

        composeTestRule.onNodeWithTag("add_sheet").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun deleteDoneButton_isDisabledWithoutDoneTodos() {
        setPage(listOf(Todo(1, "only active")))

        composeTestRule.onNodeWithTag("delete_done_button").assertIsNotEnabled()
    }

    @Test
    fun deleteDoneDialog_cancelKeepsTodos() {
        setPage()
        composeTestRule.onNodeWithTag("delete_done_button").performClick()
        composeTestRule.onNodeWithText("完了済みの Todo 2 件を削除します。この操作は元に戻せません。").assertIsDisplayed()

        composeTestRule.onNodeWithText("キャンセル").performClick()

        composeTestRule.onNodeWithText("キャンセル").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun deleteDoneDialog_confirmDeletesDoneTodos() {
        setPage()
        composeTestRule.onNodeWithTag("delete_done_button").performClick()

        composeTestRule.onNodeWithTag("confirm_delete_done").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(1)
        composeTestRule.onNodeWithText("1 件中 0 件完了").assertIsDisplayed()
        composeTestRule.onNodeWithText("完了済み 2 件を削除しました").assertIsDisplayed()
    }
}
