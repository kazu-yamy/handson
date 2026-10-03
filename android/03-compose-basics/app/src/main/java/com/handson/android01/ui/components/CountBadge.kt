package com.handson.android01.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// Box: 子要素を重ねて置く。後に書いた子ほど手前に描かれる
// contentAlignment と Modifier.align で、重ねた子の位置を指定する
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CountBadgePreview() {
    HandsonAndroid01Theme {
        CountBadge(count = 2)
    }
}
