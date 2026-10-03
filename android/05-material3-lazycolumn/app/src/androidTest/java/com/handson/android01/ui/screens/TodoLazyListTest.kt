package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// LazyColumn は画面に見えている行しか compose しない。その性質をテストで確かめる
@RunWith(AndroidJUnit4::class)
class TodoLazyListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(count: Int) {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodos(count), inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }
    }

    @Test
    fun lazyList_composesOnlyVisibleRows() {
        setScreen(1000)

        // 1000 件のデータに対して、compose されている行は画面に収まる数だけ
        val count = composeTestRule.onAllNodesWithTag("todo_row").fetchSemanticsNodes().size
        assertTrue("compose された行数: $count", count in 1..50)
    }

    @Test
    fun offscreenItem_isNotFoundUntilScrolled() {
        setScreen(1000)

        // 画面外の行はセマンティクスツリーに無い
        composeTestRule.onAllNodesWithText("Task 500").assertCountEquals(0)
    }

    @Test
    fun performScrollToIndex_showsTheItem() {
        setScreen(1000)

        composeTestRule.onNodeWithTag("todo_list").performScrollToIndex(500)

        composeTestRule.onNodeWithText("Task 501").assertIsDisplayed()
    }

    @Test
    fun performScrollToNode_showsTheItemMatchingTheCondition() {
        setScreen(1000)

        composeTestRule.onNodeWithTag("todo_list").performScrollToNode(hasText("Task 900"))

        composeTestRule.onNodeWithText("Task 900").assertIsDisplayed()
    }
}
