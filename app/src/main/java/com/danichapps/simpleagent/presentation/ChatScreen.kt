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
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()

    ChatView(
        messages = messages,
        isLoading = isLoading,
        error = error,
        isRagEnabled = isRagEnabled,
        isOfflineMode = isOfflineMode,
        maxTokens = maxTokens,
        onSendMessage = viewModel::sendMessage,
        onRagToggle = viewModel::toggleRag,
        onOfflineModeToggle = viewModel::toggleOfflineMode,
        onMaxTokensChange = viewModel::setMaxTokens
    )
}
