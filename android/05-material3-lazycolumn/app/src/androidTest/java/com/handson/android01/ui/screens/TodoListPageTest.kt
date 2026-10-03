package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 画面（TodoListPage）全体の操作をテストする。状態は画面の中にあるので、操作の結果は画面の表示で確かめる
@RunWith(AndroidJUnit4::class)
class TodoListPageTest {

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
    fun togglingRow_updatesSummary() {
        setPage()
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()

        // 2 番目の行（未完了の "build compose screen"）のチェックボックスを押す
        composeTestRule.onAllNodes(isToggleable())[1].performClick()

        composeTestRule.onNodeWithText("3 件中 3 件完了").assertIsDisplayed()
    }

    @Test
    fun addingTodo_appendsRowAndUpdatesSummary() {
        setPage()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        composeTestRule.onNodeWithText("追加").performClick()

        // 行が 1 つ増え、件数の表示も変わる。未完了の行が増えるので完了数は 2 のまま
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(4)
        composeTestRule.onNodeWithText("4 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun addingTodo_clearsInputAndDisablesAddButton() {
        setPage()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        composeTestRule.onNodeWithText("追加").assertIsEnabled()
        composeTestRule.onNodeWithText("追加").performClick()

        // 追加後は入力欄が空に戻り、空なので追加ボタンは再び無効になる
        composeTestRule.onNodeWithText("追加").assertIsNotEnabled()
    }

    @Test
    fun addButton_staysDisabledForBlankInput() {
        setPage()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("   ")

        // 空白だけでは追加できない（isNotBlank で判定している）
        composeTestRule.onNodeWithText("追加").assertIsNotEnabled()
    }

    @Test
    fun newTodo_canBeCheckedAndCountIsUpdated() {
        setPage()
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        composeTestRule.onNodeWithText("追加").performClick()

        // 追加した行（4 番目のチェックボックス）は未完了で始まる
        composeTestRule.onAllNodes(isToggleable())[3].assertIsOff()

        composeTestRule.onAllNodes(isToggleable())[3].performClick()

        // performClick の後は、テストが画面の再コンポーズ完了を待ってから検証する（waitForIdle の役割はこの後の説明）
        composeTestRule.onAllNodes(isToggleable())[3].assertIsOn()
        composeTestRule.onNodeWithText("4 件中 3 件完了").assertIsDisplayed()
    }

    @Test
    fun summaryTitle_isStillDisplayedAfterInteractions() {
        setPage()
        composeTestRule.onAllNodes(isToggleable())[1].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Todo").assertTextEquals("Todo")
    }
}
