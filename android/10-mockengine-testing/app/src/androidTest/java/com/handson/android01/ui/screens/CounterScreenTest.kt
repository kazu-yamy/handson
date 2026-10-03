package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun counter_startsAtZero() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                CounterScreen()
            }
        }

        composeTestRule.onNodeWithTag("count_text").assertTextEquals("count: 0")
    }

    @Test
    fun counter_incrementsOnEachClick() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                CounterScreen()
            }
        }

        composeTestRule.onNodeWithTag("increment_button").performClick()
        composeTestRule.onNodeWithTag("increment_button").performClick()

        composeTestRule.onNodeWithTag("count_text").assertTextEquals("count: 2")
    }
}
