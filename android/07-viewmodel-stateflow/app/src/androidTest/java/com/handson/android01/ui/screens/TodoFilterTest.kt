package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// セクション見出し・FilterChip での絞り込み・空状態
@RunWith(AndroidJUnit4::class)
class TodoFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPage(initial: List<Todo> = demoTodoList) {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListPage(viewModel = testViewModel(initial))
            }
        }
    }

    @Test
    fun allFilter_showsBothSections() {
        setPage()

        composeTestRule.onNodeWithTag("filter_All").assertIsSelected()
        composeTestRule.onNodeWithTag("header_active").assertIsDisplayed()
        composeTestRule.onNodeWithTag("header_done").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
    }

    @Test
    fun activeFilter_showsOnlyActiveSection() {
        setPage()

        composeTestRule.onNodeWithTag("filter_Active").performClick()

        composeTestRule.onNodeWithTag("filter_Active").assertIsSelected()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(1)
        composeTestRule.onNodeWithTag("header_active").assertIsDisplayed()
        composeTestRule.onNodeWithTag("header_done").assertDoesNotExist()
    }

    @Test
    fun doneFilter_showsOnlyDoneSection() {
        setPage()

        composeTestRule.onNodeWithTag("filter_Done").performClick()

        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(2)
        composeTestRule.onNodeWithTag("header_active").assertDoesNotExist()
        composeTestRule.onNodeWithTag("header_done").assertIsDisplayed()
    }

    @Test
    fun emptyFilterResult_showsEmptyState() {
        // 全部が未完了のとき、「完了」で絞り込むと空になる
        setPage(listOf(Todo(1, "only active")))

        composeTestRule.onNodeWithTag("filter_Done").performClick()

        composeTestRule.onNodeWithTag("empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("完了した Todo はありません").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(0)
    }

    @Test
    fun emptyList_showsEmptyState() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = emptyList(), inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        composeTestRule.onNodeWithText("Todo はありません").assertIsDisplayed()
    }

    @Test
    fun selectedFilter_survivesRecreation() {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            HandsonAndroid01Theme {
                TodoListPage(viewModel = testViewModel(demoTodoList))
            }
        }
        composeTestRule.onNodeWithTag("filter_Done").performClick()

        // 画面回転と同じく、保存された状態から作り直す（フィルタは rememberSaveable）
        tester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag("filter_Done").assertIsSelected()
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(2)
    }

    @Test
    fun stickyHeader_staysAtTopWhileScrolling() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodos(60), inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }

        // 未完了のセクションの途中までスクロールしても、見出しは画面の上端に残る
        composeTestRule.onNodeWithTag("todo_list").performScrollToIndex(15)

        composeTestRule.onNodeWithTag("header_active").assertIsDisplayed()
        composeTestRule.onNodeWithTag("header_done").assertDoesNotExist()
    }
}
