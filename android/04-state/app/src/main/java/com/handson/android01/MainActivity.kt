package com.handson.android01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.handson.android01.ui.screens.TodoListPage
import com.handson.android01.ui.screens.demoTodoList
import com.handson.android01.ui.theme.HandsonAndroid01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandsonAndroid01Theme {
                TodoListPage(
                    initialTodos = demoTodoList,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
