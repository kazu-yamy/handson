package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoListPageRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputText_survivesRecreation() {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            HandsonAndroid01Theme {
                TodoListPage(initialTodos = demoTodoList)
            }
        }
        // 入力欄は追加用のボトムシートの中にある。FAB で開いてから入力する
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")

        tester.emulateSavedInstanceStateRestore()

        // シートは開いたまま（表示フラグが rememberSaveable）で、入力中の文字も残り、追加ボタンは有効のまま
        composeTestRule.onNodeWithText("追加").assertIsEnabled()
    }

    @Test
    fun togglesSurviveRecreation() {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            HandsonAndroid01Theme {
                TodoListPage(initialTodos = demoTodoList)
            }
        }
        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.onNodeWithText("3 件中 3 件完了").assertIsDisplayed()

        // 画面回転と同じく、保存された状態から作り直す
        tester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("3 件中 3 件完了").assertIsDisplayed()
    }
}
