package com.handson.android01.ui.screens

import androidx.compose.ui.test.assertIsEnabled
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

// 入力中の文字・シートの表示（UI だけの状態）が、rememberSaveable で復元されること。
// 一覧と絞り込み（ViewModel の状態）の回転テストは TodoListViewModelRecreationTest にある
@RunWith(AndroidJUnit4::class)
class TodoListPageRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputText_survivesRecreation() {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            HandsonAndroid01Theme {
                TodoListPage(viewModel = testViewModel(demoTodoList))
            }
        }
        // 入力欄は追加用のボトムシートの中にある。FAB で開いてから入力する
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("todo_input").performTextInput("buy milk")

        tester.emulateSavedInstanceStateRestore()

        // シートは開いたまま（表示フラグが rememberSaveable）で、入力中の文字も残り、追加ボタンは有効のまま
        composeTestRule.onNodeWithText("追加").assertIsEnabled()
    }
}
