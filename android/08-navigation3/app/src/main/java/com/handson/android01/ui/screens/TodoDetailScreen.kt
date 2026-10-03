package com.handson.android01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.kotlin.Todo
import com.handson.android01.kotlin.toTitleCase
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// stateless な詳細画面。todo が null のときは「見つからない」表示にする（一覧で消えたあと、など）。
// 戻る操作は onBack で呼び出し側に任せる。この画面は、自分がバックスタックのどこにあるかを知らない
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    todo: Todo?,
    onBack: () -> Unit,
    onToggle: (done: Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Todo の詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
                        // AutoMirrored: 右から左に書く言語では、矢印の向きが自動で反転する
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            if (todo == null) {
                Text(
                    text = "この Todo は見つかりません",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("detail_not_found"),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = todo.title.toTitleCase(),
                        modifier = Modifier.testTag("detail_title"),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = todo.done,
                            onCheckedChange = onToggle,
                            modifier = Modifier.testTag("detail_checkbox"),
                        )
                        Text(
                            text = if (todo.done) "完了" else "未完了",
                            modifier = Modifier.testTag("detail_status"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    // 削除すると、この Todo を見る画面の意味が無くなるので、呼び出し側が一覧へ戻る
                    OutlinedButton(onClick = onDelete, modifier = Modifier.testTag("detail_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("削除")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailScreenPreview() {
    HandsonAndroid01Theme {
        TodoDetailScreen(todo = demoTodoList[1], onBack = {}, onToggle = {}, onDelete = {})
    }
}
