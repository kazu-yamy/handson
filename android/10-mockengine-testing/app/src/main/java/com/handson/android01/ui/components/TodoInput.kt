package com.handson.android01.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// TextFieldState: 入力中の文字列を持つ新しい状態のクラス（foundation 1.7 以降）。value / onValueChange の代わりに使える
@Composable
fun TodoInput(
    state: TextFieldState,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canAdd = state.text.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            state = state,
            modifier = Modifier
                .weight(1f)
                .testTag("todo_input"),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("新しい Todo") },
        )
        Button(
            onClick = onAddClick,
            enabled = canAdd,
            modifier = Modifier
                .padding(start = 8.dp)
                .testTag("add_button"),
        ) {
            Text("追加")
        }
    }
}
