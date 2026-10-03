package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun titleAndSummary_areDisplayed() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onNodeWithText("Todo").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
    }

    @Test
    fun todoRows_countMatchesData() {
        // testTag("todo_row") を持つ行の数で、一覧に全件が並んでいるかを確かめる
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun doneStatus_appearsOncePerDoneTodo() {
        // テキストの完全一致で数える。「未完了」は「完了」と一致しないので数えられない。
        // 見出し（「未完了」「完了」）とフィルタのチップにも同じ文字があるので、行（todo_row）の中だけを数える
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onAllNodes(hasText("完了") and hasAnyAncestor(hasTestTag("todo_row"))).assertCountEquals(2)
        composeTestRule.onAllNodes(hasText("未完了") and hasAnyAncestor(hasTestTag("todo_row"))).assertCountEquals(1)
    }

    @Test
    fun emptyList_showsZeroRows() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = emptyList(), inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onNodeWithText("0 件中 0 件完了").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(0)
    }

    @Test
    fun addButton_callsOnAddClick() {
        // 状態を持たない画面なので、クリックの回数はテスト側の変数で受け取る
        var clickCount = 0
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = { clickCount++ }, showAddSheet = true)
            }
        }

        // 空のままでは追加ボタンは無効（後述のテスト）。文字を入れてから押す
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")
        // キーボードが出るとシートが持ち上がり、タップの座標がずれることがある。
        // 位置に関係なくクリックの処理を呼ぶ
        composeTestRule.onNodeWithText("追加").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, clickCount)
    }

    @Test
    fun addButton_isDisabledWhenInputIsEmpty() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {}, showAddSheet = true)
            }
        }

        composeTestRule.onNodeWithText("追加").assertIsNotEnabled()
    }

    @Test
    fun semanticsTree_canBePrintedToLog() {
        // printToLog: セマンティクスツリーを Logcat に出す。どのノードにどのテキストがあるかを確かめる時に使う
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onRoot().printToLog("TodoListScreenTree")
    }
}
