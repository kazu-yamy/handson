package com.handson.android01.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todoRow_showsTitleAndStatus() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoRow(todo = Todo(1, "learn kotlin basics", done = true), onToggle = {})
            }
        }

        composeTestRule.onNodeWithText("Learn Kotlin Basics").assertIsDisplayed()
        composeTestRule.onNodeWithText("完了").assertIsDisplayed()
    }

    @Test
    fun todoRow_fillsParentWidth() {
        // 親の幅を 300dp に固定すると、fillMaxWidth の結果が 300dp になる
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                Box(modifier = Modifier.width(300.dp)) {
                    TodoRow(todo = Todo(2, "build compose screen"), onToggle = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("todo_row").assertWidthIsEqualTo(300.dp)
    }

    @Test
    fun todoRow_callsOnToggleWithNewValue() {
        var received: Boolean? = null
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                TodoRow(todo = Todo(3, "write unit tests", done = true), onToggle = { received = it })
            }
        }

        // 行は完了状態を持たないので、押しても表示は変わらない。通知された値（false）だけを確かめる
        composeTestRule.onNode(isToggleable()).performClick()

        assertEquals(false, received)
    }
}
