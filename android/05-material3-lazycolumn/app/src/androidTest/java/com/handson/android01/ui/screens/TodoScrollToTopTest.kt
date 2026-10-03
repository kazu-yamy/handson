package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 「先頭へ」ボタンは、先頭の行が見えている間は出さない（derivedStateOf で出し分ける）
@RunWith(AndroidJUnit4::class)
class TodoScrollToTopTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoListScreen(todos = demoTodos(50), inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
            }
        }
    }

    @Test
    fun toTopButton_isHiddenAtTop() {
        setScreen()

        composeTestRule.onNodeWithTag("scroll_to_top").assertDoesNotExist()
    }

    @Test
    fun toTopButton_appearsAfterScrolling() {
        setScreen()

        composeTestRule.onNodeWithTag("todo_list").performScrollToIndex(30)

        composeTestRule.onNodeWithTag("scroll_to_top").assertIsDisplayed()
    }

    @Test
    fun toTopButton_scrollsBackAndDisappears() {
        setScreen()
        composeTestRule.onNodeWithTag("todo_list").performScrollToIndex(30)

        composeTestRule.onNodeWithTag("scroll_to_top").performClick()

        // animateScrollToItem(0) が終わると先頭の行が見え、ボタンは消える
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scroll_to_top").assertDoesNotExist()
    }
}
