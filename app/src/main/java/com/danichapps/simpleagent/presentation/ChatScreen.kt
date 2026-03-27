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
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
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

    ChatView(
        messages = messages,
        isLoading = isLoading,
        error = error,
        isRagEnabled = isRagEnabled,
        isRagIndexed = isRagIndexed,
        ragFolderName = ragFolderName,
        modelFileName = modelFileName,
        isOfflineMode = isOfflineMode,
        modelState = modelState,
        onSendMessage = viewModel::sendMessage,
        onRagToggle = viewModel::toggleRag,
        onOfflineModeToggle = viewModel::toggleOfflineMode,
        onPickRagFolder = { folderPicker.launch(null) },
        onPickModelFile = { modelPicker.launch(arrayOf("*/*")) }
    )
}
