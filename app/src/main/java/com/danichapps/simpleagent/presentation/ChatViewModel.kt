package com.danichapps.simpleagent.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danichapps.simpleagent.data.local.ModelSelectionManager
import com.danichapps.simpleagent.data.local.RagFolderPreferences
import com.danichapps.simpleagent.data.local.EmbeddingModelSelectionManager
import com.danichapps.simpleagent.data.local.ChatTuningSettingsStore
import com.danichapps.simpleagent.data.remote.LlamaCppEmbeddingService
import com.danichapps.simpleagent.data.remote.OnDeviceLlamaCppService
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import com.danichapps.simpleagent.domain.usecase.ExtractTaskStateUseCase
import com.danichapps.simpleagent.domain.usecase.OfflineSendMessageUseCase
import com.danichapps.simpleagent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_HISTORY = 20
private const val MAX_HISTORY_OFFLINE = 6

sealed class ModelState {
    object NotReady : ModelState()
    object Initializing : ModelState()
    object Indexing : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ChatViewModel(
    private val openAiChatRepo: ChatRepository,
    private val onDeviceChatRepo: ChatRepository,
    private val onDeviceLlamaCppService: OnDeviceLlamaCppService,
    private val llamaCppEmbeddingService: LlamaCppEmbeddingService,
    private val onlineSendMessageUseCase: SendMessageUseCase,
    private val offlineSendMessageUseCase: OfflineSendMessageUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase,
    private val localRagRepository: RagRepository,
    private val ragFolderPreferences: RagFolderPreferences,
    private val modelSelectionManager: ModelSelectionManager,
    private val embeddingModelSelectionManager: EmbeddingModelSelectionManager,
    private val chatTuningSettingsStore: ChatTuningSettingsStore
) : ViewModel() {

    private val _taskState = MutableStateFlow(TaskState())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRagEnabled = MutableStateFlow(false)
    val isRagEnabled: StateFlow<Boolean> = _isRagEnabled.asStateFlow()

    private val _isRagIndexed = MutableStateFlow(localRagRepository.isIndexed())
    val isRagIndexed: StateFlow<Boolean> = _isRagIndexed.asStateFlow()

    private val _ragFolderName = MutableStateFlow(ragFolderPreferences.getDisplayName())
    val ragFolderName: StateFlow<String?> = _ragFolderName.asStateFlow()

    private val _modelFileName = MutableStateFlow(modelSelectionManager.getSelectedModelDisplayName())
    val modelFileName: StateFlow<String?> = _modelFileName.asStateFlow()

    private val _embeddingModelFileName = MutableStateFlow(embeddingModelSelectionManager.getSelectedModelDisplayName())
    val embeddingModelFileName: StateFlow<String?> = _embeddingModelFileName.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotReady)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _chatTuningSettings = MutableStateFlow(chatTuningSettingsStore.load())
    val chatTuningSettings: StateFlow<ChatTuningSettings> = _chatTuningSettings.asStateFlow()

    fun updateTemperature(value: Float) {
        val updated = _chatTuningSettings.value.copy(temperature = value.coerceIn(0f, 2f))
        _chatTuningSettings.value = updated
        chatTuningSettingsStore.save(updated)
    }

    fun updateMaxTokens(value: String) {
        val parsed = value.toIntOrNull() ?: return
        val updated = _chatTuningSettings.value.copy(maxTokens = parsed.coerceIn(16, 512))
        _chatTuningSettings.value = updated
        chatTuningSettingsStore.save(updated)
    }

    fun updateSystemPrompt(value: String) {
        val updated = _chatTuningSettings.value.copy(systemPrompt = value)
        _chatTuningSettings.value = updated
        chatTuningSettingsStore.save(updated)
    }

    fun toggleRag(enabled: Boolean) {
        _isRagEnabled.value = enabled
    }

    fun setRagFolder(uri: Uri, displayName: String?) {
        ragFolderPreferences.save(uri, displayName)
        _ragFolderName.value = displayName
        _isRagIndexed.value = localRagRepository.isIndexed()
        if (_isOfflineMode.value) {
            initializeModel()
        }
    }

    fun importModel(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            try {
                _modelState.value = ModelState.Initializing
                onDeviceLlamaCppService.release()
                val importedFile = modelSelectionManager.importModel(uri, displayName)
                _modelFileName.value = displayName ?: importedFile.name
                if (_isOfflineMode.value) {
                    initializeModel()
                } else {
                    _modelState.value = ModelState.NotReady
                    _error.value = "Модель выбрана: ${importedFile.name}"
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Не удалось импортировать модель")
            }
        }
    }

    fun importEmbeddingModel(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            try {
                _modelState.value = ModelState.Initializing
                llamaCppEmbeddingService.release()
                val importedFile = embeddingModelSelectionManager.importModel(uri, displayName)
                _embeddingModelFileName.value = displayName ?: importedFile.name
                if (_isOfflineMode.value && ragFolderPreferences.hasFolder()) {
                    initializeModel()
                } else {
                    _modelState.value = ModelState.NotReady
                    _error.value = "Embedding-модель выбрана: ${importedFile.name}"
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Не удалось импортировать embedding-модель")
            }
        }
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        if (enabled && _modelState.value !is ModelState.Ready && _modelState.value !is ModelState.Initializing && _modelState.value !is ModelState.Indexing) {
            initializeModel()
        }
    }

    private fun initializeModel() {
        viewModelScope.launch {
            _modelState.value = ModelState.Initializing
            try {
                onDeviceLlamaCppService.initialize()
                if (ragFolderPreferences.hasFolder() && !localRagRepository.isIndexed()) {
                    _modelState.value = ModelState.Indexing
                    localRagRepository.buildIndexIfNeeded()
                }
                _isRagIndexed.value = localRagRepository.isIndexed()
                _modelState.value = ModelState.Ready
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(
                    e.message ?: "Не удалось инициализировать on-device llama.cpp"
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        if (_isRagEnabled.value && !ragFolderPreferences.hasFolder()) {
            _error.value = "Выберите папку базы RAG перед поиском по документам."
            return
        }
        viewModelScope.launch {
            val userMsg = Message(role = "user", content = text)
            val historyWithUser = (_messages.value + userMsg).takeLast(MAX_HISTORY)
            _messages.value = historyWithUser
            _isLoading.value = true
            _error.value = null

            if (_isOfflineMode.value) {
                sendOffline(historyWithUser)
            } else {
                sendOnline(historyWithUser)
            }
        }
    }

    private suspend fun sendOnline(history: List<Message>) {
        try {
            val (answer, sources) = onlineSendMessageUseCase(
                history,
                chatRepository = openAiChatRepo,
                ragEnabled = _isRagEnabled.value,
                taskState = _taskState.value,
                settings = _chatTuningSettings.value
            )
            val updatedHistory = (history + Message(role = "assistant", content = answer, sources = sources))
                .takeLast(MAX_HISTORY)
            _messages.value = updatedHistory
            viewModelScope.launch {
                try {
                    _taskState.value = extractTaskStateUseCase(updatedHistory, _taskState.value, openAiChatRepo)
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Ошибка соединения"
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun sendOffline(history: List<Message>) {
        if (_modelState.value !is ModelState.Ready) {
            _error.value = "Локальная модель не готова. Убедитесь, что GGUF лежит на телефоне и native llama.cpp подключён."
            _isLoading.value = false
            return
        }
        try {
            val (answer, sources) = offlineSendMessageUseCase(
                history,
                chatRepository = onDeviceChatRepo,
                ragEnabled = _isRagEnabled.value,
                taskState = _taskState.value,
                settings = _chatTuningSettings.value
            )
            if (answer.isNotBlank()) {
                val updatedHistory = (history + Message(role = "assistant", content = answer, sources = sources))
                    .takeLast(MAX_HISTORY_OFFLINE)
                _messages.value = updatedHistory
            } else {
                _error.value = "Локальная модель не вернула ответ"
                _messages.value = history.dropLast(1)
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Ошибка локальной генерации"
        } finally {
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        onDeviceLlamaCppService.release()
        llamaCppEmbeddingService.release()
    }
}
