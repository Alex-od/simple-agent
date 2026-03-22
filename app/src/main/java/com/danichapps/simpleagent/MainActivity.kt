package com.danichapps.simpleagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danichapps.simpleagent.presentation.ChatScreen
import com.danichapps.simpleagent.ui.theme.SimpleAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleAgentTheme {
                ChatScreen()
            }
        }
    }
}
