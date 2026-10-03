package com.handson.android01.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// Composable 関数: UI を「宣言」する関数。値（View オブジェクトなど）を返さず、Unit を返す。
// 命名は PascalCase（先頭大文字）にする。Compose コンパイラがこの関数を Composable として扱う
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // modifier は最初の省略可能な引数にする（Compose の慣習）。呼び出し側が外から見た目を足せるようにする
    Text(
        text = "Hello, $name!",
        modifier = modifier,
    )
}

// Preview は引数を取らない Composable にする（@PreviewParameter を除く。ステップ 5 で扱う）
@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    HandsonAndroid01Theme {
        Greeting(name = "Android")
    }
}
