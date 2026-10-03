package com.handson.android01.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.kotlin.Todo
import com.handson.android01.kotlin.toTitleCase
import com.handson.android01.ui.theme.DoneGreen40
import com.handson.android01.ui.theme.DoneGreen80
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// onToggle: チェックボックスが押されたときに、新しい完了状態（true/false）を親に知らせる。行自身は完了状態を持たない
@Composable
fun TodoRow(
    todo: Todo,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    // テーマの背景色の明るさから、ダークかどうかを判定する（端末設定ではなく実際に使われている色で判断する）
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val doneColor = if (isDark) DoneGreen80 else DoneGreen40

    // Row: 子要素を左から右に並べる。verticalAlignment で高さ方向の揃え位置を決める
    Row(
        // testTag は padding より前に書く。後ろに書くと、タグの付いたノードが余白の内側だけになる
        modifier = modifier
            .fillMaxWidth()
            .testTag("todo_row")
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // checked は親から受け取った値をそのまま表示する。変更は onToggle で親に任せる（状態は下へ、イベントは上へ）
        Checkbox(checked = todo.done, onCheckedChange = onToggle)
        // weight(1f): 残りの横幅をすべて使う。weight を持つ子が複数あれば比率で分ける
        Text(
            text = todo.title.toTitleCase(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = if (todo.done) "完了" else "未完了",
            color = if (todo.done) doneColor else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        // onDelete が渡されたときだけ、削除ボタンを出す。スワイプが使えない人のための操作でもある
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoRowPreview() {
    HandsonAndroid01Theme {
        TodoRow(todo = Todo(1, "learn kotlin basics", done = true), onToggle = {})
    }
}

// ダークテーマの確認用。uiMode を指定すると、Preview の中で isSystemInDarkTheme() 相当の判定が暗くなる
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodoRowDarkPreview() {
    HandsonAndroid01Theme {
        TodoRow(todo = Todo(1, "learn kotlin basics", done = true), onToggle = {})
    }
}
