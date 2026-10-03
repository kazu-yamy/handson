package com.handson.android01.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GreetingTest {

    // createComposeRule(): Activity を起動せず、テスト対象の Composable だけを setContent で描画する
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_showsName() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                Greeting(name = "Compose")
            }
        }

        composeTestRule.onNodeWithText("Hello, Compose!").assertIsDisplayed()
    }

    @Test
    fun greeting_showsDifferentNameWhenCalledWithAnotherArgument() {
        composeTestRule.setContent {
            HandsonAndroid01Theme {
                Greeting(name = "Kotlin")
            }
        }

        composeTestRule.onNodeWithText("Hello, Kotlin!").assertIsDisplayed()
    }
}
