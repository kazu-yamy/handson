package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 読み込み中の表示
@RunWith(AndroidJUnit4::class)
class TodoLoadingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_whenLoading_showsOnlyIndicator() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(
                    todos = emptyList(),
                    inputState = rememberTextFieldState(),
                    onToggle = { _, _ -> },
                    onAddClick = {},
                    isLoading = true,
                )
            }
        }

        composeTestRule.onNodeWithTag("loading").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("empty_state").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(0)
        // 操作部品とサマリは出さない
        composeTestRule.onAllNodesWithTag("fab_add").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_done_button").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("filter_All").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("0 件中 0 件完了").assertCountEquals(0)
    }

    @Test
    fun page_showsIndicatorUntilLoaded_thenShowsRows() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                // 待ち時間は viewModelScope の delay（実時間）。Compose のテスト用クロックでは進まないので、waitUntil で待つ
                TodoListPage(viewModel = testViewModel(loadDelayMillis = 500L))
            }
        }
        composeTestRule.onNodeWithTag("loading").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("fab_add").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("0 件中 0 件完了").assertCountEquals(0)

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("todo_row").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("loading").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
        composeTestRule.onNodeWithTag("fab_add").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
    }
}
