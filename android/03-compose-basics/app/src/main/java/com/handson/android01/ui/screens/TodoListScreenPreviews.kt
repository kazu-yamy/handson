package com.handson.android01.ui.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// 複数のデータで同じ画面を確認するための提供元。values の要素ごとに Preview が 1 つずつ描かれる
class TodoListPreviewProvider : PreviewParameterProvider<List<Todo>> {
    override val values: Sequence<List<Todo>> = sequenceOf(
        demoTodoList,
        emptyList(),
        List(12) { index -> Todo(index + 1, "task ${index + 1}", done = index % 3 == 0) },
    )
}

// @PreviewParameter: 引数に値を受け取る。引数付きの Preview はこの指定のときだけ使える
@Preview(name = "parameter", showBackground = true)
@Composable
private fun TodoListScreenParameterPreview(
    @PreviewParameter(TodoListPreviewProvider::class) todos: List<Todo>,
) {
    HandsonAndroid01Theme {
        TodoListScreen(todos = todos, onAddClick = {})
    }
}

// fontScale: 文字サイズを拡大した状態で崩れないか確認する
@Preview(name = "fontScale 1.5", showBackground = true, fontScale = 1.5f)
@Composable
private fun TodoListScreenFontScalePreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}

// device: 端末の画面サイズを指定する（Devices には定義済みの機種名がある）
@Preview(name = "tablet", device = Devices.TABLET, showBackground = true)
@Composable
private fun TodoListScreenTabletPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}

// uiMode: ダークテーマの確認。マルチプレビュー @PreviewLightDark は明/暗の 2 つを一度に作る
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TodoListScreenDarkPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}

// マルチプレビュー: 1 つのアノテーションで複数の条件の Preview を作る
@PreviewLightDark
@Composable
private fun TodoListScreenLightDarkMultiPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}

@PreviewFontScale
@Composable
private fun TodoListScreenFontScaleMultiPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}

@PreviewScreenSizes
@Composable
private fun TodoListScreenScreenSizesMultiPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, onAddClick = {})
    }
}
