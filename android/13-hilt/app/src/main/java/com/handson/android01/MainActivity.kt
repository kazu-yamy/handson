package com.handson.android01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.handson.android01.ui.navigation.TodoApp
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import dagger.hilt.android.AndroidEntryPoint

// Hilt から部品を受け取る Activity には、この注釈を付ける
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandsonAndroid01Theme {
                TodoApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
