package com.handson.android01.ui.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun display_showsGivenCount() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                CounterDisplay(count = 5, onIncrement = {})
            }
        }

        composeTestRule.onNodeWithTag("count_text").assertTextEquals("count: 5")
    }

    @Test
    fun display_callsOnIncrementAndKeepsCount() {
        var clickCount = 0
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                CounterDisplay(count = 5, onIncrement = { clickCount++ })
            }
        }

        composeTestRule.onNodeWithTag("increment_button").performClick()

        // 部品は値を持たないので、クリックしても表示は 5 のまま。増やすのは親の仕事
        assertEquals(1, clickCount)
        composeTestRule.onNodeWithTag("count_text").assertTextEquals("count: 5")
    }
}
