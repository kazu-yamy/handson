package com.handson.android01.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Modifier は左から順に外側から包んでいく。順序を変えると見た目が変わる
// 1 つ目: padding が先 → 余白の外側に背景色が付かない（背景は文字のまわりだけ）
@Preview(name = "padding → background", showBackground = true)
@Composable
private fun PaddingThenBackgroundPreview() {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .background(Color(0xFFFFE082)),
    ) {
        Text("A")
    }
}

// 2 つ目: background が先 → 余白の部分まで背景色が付く
@Preview(name = "background → padding", showBackground = true)
@Composable
private fun BackgroundThenPaddingPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFE082))
            .padding(24.dp),
    ) {
        Text("A")
    }
}
