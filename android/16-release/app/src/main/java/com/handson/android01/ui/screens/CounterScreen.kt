package com.handson.android01.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.handson.android01.ui.components.CounterDisplay

// stateful（状態を持つ）な画面。count の状態をここで持ち、部品には値とイベントだけを渡す
// 状態を持つ場所を 1 か所に決めると、部品の動きを状態に関係なくテストできる
@Composable
fun CounterScreen(modifier: Modifier = Modifier) {
    // rememberSaveable: remember と同じに値を保持するうえ、画面回転などの構成変更をまたいでも値を保存して戻す
    var count by rememberSaveable { mutableStateOf(0) }

    SideEffect {
        Log.d("Recompose", "CounterScreen: count=$count")
    }

    CounterDisplay(
        count = count,
        onIncrement = { count++ },
        modifier = modifier,
    )
}
