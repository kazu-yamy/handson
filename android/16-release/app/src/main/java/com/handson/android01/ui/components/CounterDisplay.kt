package com.handson.android01.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// stateless（状態を持たない）な部品。表示する値は count で受け取り、操作は onIncrement で親に知らせる
// 値を持たないので、同じ引数を渡せば何度呼んでも同じ画面になる。テストや Preview で扱いやすい
@Composable
fun CounterDisplay(
    count: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "count: $count",
            modifier = Modifier.testTag("count_text"),
            style = MaterialTheme.typography.titleLarge,
        )
        // ボタンを押しても count は変わらない。onIncrement を呼ぶだけで、増やすかどうかは親が決める
        Button(
            onClick = onIncrement,
            modifier = Modifier.testTag("increment_button"),
        ) {
            Text("増やす")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CounterDisplayPreview() {
    HandsonAndroid01Theme {
        CounterDisplay(count = 3, onIncrement = {})
    }
}
