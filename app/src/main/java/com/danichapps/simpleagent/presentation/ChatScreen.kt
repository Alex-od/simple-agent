package com.danichapps.simpleagent.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel = koinViewModel()) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRagEnabled by viewModel.isRagEnabled.collectAsState()
    val isRagIndexed by viewModel.isRagIndexed.collectAsState()
    val ragFolderName by viewModel.ragFolderName.collectAsState()
    val modelFileName by viewModel.modelFileName.collectAsState()
    val embeddingModelFileName by viewModel.embeddingModelFileName.collectAsState()
    val chatMode by viewModel.chatMode.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val chatTuningSettings by viewModel.chatTuningSettings.collectAsState()
    val localServerSettings by viewModel.localServerSettings.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val displayName = DocumentFile.fromTreeUri(context, uri)?.name
            viewModel.setRagFolder(uri, displayName)
        }
    }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val displayName = DocumentFile.fromSingleUri(context, uri)?.name
            viewModel.importModel(uri, displayName)
        }
    }
    val embeddingModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val displayName = DocumentFile.fromSingleUri(context, uri)?.name
            viewModel.importEmbeddingModel(uri, displayName)
        }
    }

    ChatView(
        messages = messages,
        isLoading = isLoading,
        error = error,
        isRagEnabled = isRagEnabled,
        isRagIndexed = isRagIndexed,
        ragFolderName = ragFolderName,
        modelFileName = modelFileName,
        embeddingModelFileName = embeddingModelFileName,
        chatMode = chatMode,
        modelState = modelState,
        chatTuningSettings = chatTuningSettings,
        localServerSettings = localServerSettings,
        onSendMessage = viewModel::sendMessage,
        onRagToggle = viewModel::toggleRag,
        onChatModeChange = viewModel::setChatMode,
        onTemperatureChange = viewModel::updateTemperature,
        onMaxTokensChange = viewModel::updateMaxTokens,
        onSystemPromptChange = viewModel::updateSystemPrompt,
        onServerUrlChange = viewModel::updateServerBaseUrl,
        onServerModelChange = viewModel::updateServerModel,
        onPickRagFolder = { folderPicker.launch(null) },
        onPickModelFile = { modelPicker.launch(arrayOf("*/*")) },
        onPickEmbeddingModelFile = { embeddingModelPicker.launch(arrayOf("*/*")) }
    )
}
