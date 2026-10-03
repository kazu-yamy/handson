package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 読み込みの失敗と再試行の表示。ネットワークには出ない（偽のサーバーが 500 を返す）
@RunWith(AndroidJUnit4::class)
class TodoErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val server = FakeTodoServer(status = HttpStatusCode.InternalServerError)

    // 応答は別のスレッドから届き、Compose のテスト用クロックでは進まないので、waitUntil で待つ
    private fun waitForTag(tag: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun screen_withErrorMessage_showsMessageAndRetryOnly() {
        var retryCount = 0
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(
                    todos = emptyList(),
                    inputState = rememberTextFieldState(),
                    onToggle = { _, _ -> },
                    onAddClick = {},
                    errorMessage = "ネットワークに接続できません",
                    onRetry = { retryCount++ },
                )
            }
        }

        composeTestRule.onNodeWithTag("error_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("ネットワークに接続できません").assertIsDisplayed()
        // 読み込み中と同じく、操作部品・サマリ・空状態は出さない
        composeTestRule.onAllNodesWithTag("loading").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("empty_state").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("fab_add").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_done_button").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("filter_All").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("0 件中 0 件完了").assertCountEquals(0)

        composeTestRule.onNodeWithTag("retry_button").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun page_whenLoadFails_showsErrorMessage() {
        composeTestRule.setContent {
            HandsonAndroid01Theme { TodoListPage(viewModel = testViewModel(api = server.api)) }
        }

        waitForTag("error_state")

        composeTestRule.onNodeWithText("サーバーがエラーを返しました（HTTP 500）").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("loading").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("fab_add").assertCountEquals(0)
    }

    @Test
    fun page_retryAfterFailure_showsRows() {
        composeTestRule.setContent {
            HandsonAndroid01Theme { TodoListPage(viewModel = testViewModel(api = server.api)) }
        }
        waitForTag("error_state")

        // サーバーが直ったことにして、「再試行」を押す
        server.status = HttpStatusCode.OK
        composeTestRule.onNodeWithTag("retry_button").performClick()
        waitForTag("todo_row")

        composeTestRule.onAllNodesWithTag("error_state").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("todo_row").assertCountEquals(3)
        composeTestRule.onNodeWithTag("fab_add").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 件中 2 件完了").assertIsDisplayed()
        assertEquals(2, server.requestCount)
    }
}
