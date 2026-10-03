package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
                TodoListScreen(todos = demoTodoList, onAddClick = {})
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
                TodoListScreen(todos = demoTodoList, onAddClick = {})
            }
        }

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun doneStatus_appearsOncePerDoneTodo() {
        // テキストの完全一致で数える。「未完了」は「完了」と一致しないので数えられない
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, onAddClick = {})
            }
        }

        composeTestRule.onAllNodesWithText("完了").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("未完了").assertCountEquals(1)
    }

    @Test
    fun emptyList_showsZeroRows() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = emptyList(), onAddClick = {})
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
                TodoListScreen(todos = demoTodoList, onAddClick = { clickCount++ })
            }
        }

        composeTestRule.onNodeWithText("追加").performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun semanticsTree_canBePrintedToLog() {
        // printToLog: セマンティクスツリーを Logcat に出す。どのノードにどのテキストがあるかを確かめる時に使う
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodoList, onAddClick = {})
            }
        }

        composeTestRule.onRoot().printToLog("TodoListScreenTree")
    }
}
