package com.handson.android01.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.performSemanticsAction
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

        // 一覧は「未完了」「完了」の順なので、先頭の行が未完了の "build compose screen"。そのチェックボックスを押す
        composeTestRule.onAllNodes(isToggleable())[0].performClick()

        composeTestRule.onNodeWithText("3 件中 3 件完了").assertIsDisplayed()
    }

    @Test
    fun addingTodo_appendsRowAndUpdatesSummary() {
        setPage()
        // 入力欄は、追加用のボトムシートの中にある。FAB で開いてから操作する
        composeTestRule.onNodeWithTag("fab_add").performClick()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        // キーボードが出るとシートが持ち上がり、タップの座標がずれることがある。
        // 位置に関係なくクリックの処理を呼ぶ
        composeTestRule.onNodeWithText("追加").performSemanticsAction(SemanticsActions.OnClick)

        // 行が 1 つ増え、件数の表示も変わる。未完了の行が増えるので完了数は 2 のまま
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(4)
        composeTestRule.onNodeWithText("4 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun addingTodo_closesSheetAndClearsInput() {
        setPage()
        // 入力欄は、追加用のボトムシートの中にある。FAB で開いてから操作する
        composeTestRule.onNodeWithTag("fab_add").performClick()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        composeTestRule.onNodeWithText("追加").assertIsEnabled()
        composeTestRule.onNodeWithText("追加").performSemanticsAction(SemanticsActions.OnClick)

        // 追加するとシートが閉じる（入力欄が画面から消える）
        composeTestRule.onNodeWithTag("add_sheet").assertDoesNotExist()

        // 開き直すと入力欄は空に戻っていて、空なので追加ボタンは無効
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithText("追加").assertIsNotEnabled()
    }

    @Test
    fun addButton_staysDisabledForBlankInput() {
        setPage()
        // 入力欄は、追加用のボトムシートの中にある。FAB で開いてから操作する
        composeTestRule.onNodeWithTag("fab_add").performClick()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("   ")

        // 空白だけでは追加できない（isNotBlank で判定している）
        composeTestRule.onNodeWithText("追加").assertIsNotEnabled()
    }

    @Test
    fun newTodo_canBeCheckedAndCountIsUpdated() {
        setPage()
        // 入力欄は、追加用のボトムシートの中にある。FAB で開いてから操作する
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        composeTestRule.onNodeWithText("追加").performSemanticsAction(SemanticsActions.OnClick)

        // 追加した行は未完了で始まる。未完了のセクションの 2 番目（全体の 2 番目のチェックボックス）に並ぶ
        composeTestRule.onAllNodes(isToggleable())[1].assertIsOff()

        composeTestRule.onAllNodes(isToggleable())[1].performClick()

        // 完了にすると、行は「完了」のセクションの末尾（全体の 4 番目）へ移る
        composeTestRule.onAllNodes(isToggleable())[3].assertIsOn()
        composeTestRule.onNodeWithText("4 件中 3 件完了").assertIsDisplayed()
    }

    @Test
    fun summaryTitle_isStillDisplayedAfterInteractions() {
        setPage()
        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Todo").assertTextEquals("Todo")
    }
}
