package com.handson.android01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.kotlin.Todo
import com.handson.android01.kotlin.summaryLine
import com.handson.android01.kotlin.toTitleCase
import com.handson.android01.ui.theme.HandsonAndroid01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandsonAndroid01Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// 画面に出す Todo（Step 3 の Todo を Step 4 の sampleTodos と同じ形で並べる）
private val demoTodos = listOf(
    Todo(1, "learn kotlin basics", done = true),
    Todo(2, "build compose screen"),
    Todo(3, "write unit tests", done = true),
)

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = demoTodos.summaryLine())
        demoTodos.forEach { todo ->
            val mark = if (todo.done) "[x]" else "[ ]"
            Text(text = "$mark ${todo.title.toTitleCase()}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HandsonAndroid01Theme {
        Greeting()
    }
}
