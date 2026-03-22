package com.danichapps.simpleagent.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel = koinViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRagEnabled by viewModel.isRagEnabled.collectAsState()

    ChatView(
        messages = messages,
        isLoading = isLoading,
        error = error,
        isRagEnabled = isRagEnabled,
        onSendMessage = viewModel::sendMessage,
        onRagToggle = viewModel::toggleRag
    )
}
