package com.danichapps.simpleagent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danichapps.simpleagent.data.remote.DownloadState
import com.danichapps.simpleagent.data.remote.ModelDownloadManager
import com.danichapps.simpleagent.data.remote.OnDeviceLlmService
import com.danichapps.simpleagent.data.remote.StorageOption
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.usecase.ExtractTaskStateUseCase
import com.danichapps.simpleagent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_HISTORY = 20

sealed class ModelState {
    object NotDownloaded : ModelState()
    data class AskingStorageLocation(val options: List<StorageOption>) : ModelState()
    data class Downloading(val progress: Int, val downloadedMb: Int, val totalMb: Int) : ModelState()
    object Initializing : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ChatViewModel(
    private val openAiChatRepo: ChatRepository,
    private val onDeviceChatRepo: ChatRepository,
    private val onDeviceLlmService: OnDeviceLlmService,
    private val modelDownloadManager: ModelDownloadManager,
    private val sendMessageUseCase: SendMessageUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase
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

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(
        if (modelDownloadManager.isModelDownloaded()) ModelState.NotDownloaded
        else ModelState.NotDownloaded
    )
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    init {
        // Если модель уже скачана — сразу инициализируем
        if (modelDownloadManager.isModelDownloaded()) {
            initializeModel()
        }
    }

    fun toggleRag(enabled: Boolean) {
        _isRagEnabled.value = enabled
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        if (enabled && _modelState.value is ModelState.NotDownloaded) {
            _modelState.value = ModelState.AskingStorageLocation(modelDownloadManager.getStorageOptions())
        }
    }

    fun onStorageLocationSelected(path: String) {
        viewModelScope.launch {
            modelDownloadManager.selectedStoragePath = path
            modelDownloadManager.downloadModel(path).collect { state ->
                when (state) {
                    is DownloadState.Downloading -> _modelState.value =
                        ModelState.Downloading(state.progress, state.downloadedMb, state.totalMb)
                    is DownloadState.Done -> initializeModel()
                    is DownloadState.Error -> _modelState.value = ModelState.Error(state.message)
                }
            }
        }
    }

    private fun initializeModel() {
        viewModelScope.launch {
            _modelState.value = ModelState.Initializing
            try {
                onDeviceLlmService.initialize()
                _modelState.value = ModelState.Ready
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Ошибка инициализации модели")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        viewModelScope.launch {
            val userMsg = Message(role = "user", content = text)
            val historyWithUser = (_messages.value + userMsg).takeLast(MAX_HISTORY)
            _messages.value = historyWithUser
            _isLoading.value = true
            _error.value = null

            if (_isOfflineMode.value) {
                sendOfflineStreaming(historyWithUser)
            } else {
                sendOnline(historyWithUser)
            }
        }
    }

    private suspend fun sendOnline(history: List<Message>) {
        try {
            val (answer, sources) = sendMessageUseCase(
                history,
                chatRepository = openAiChatRepo,
                ragEnabled = _isRagEnabled.value,
                taskState = _taskState.value
            )
            val updatedHistory = (history + Message(role = "assistant", content = answer, sources = sources))
                .takeLast(MAX_HISTORY)
            _messages.value = updatedHistory
            viewModelScope.launch {
                try {
                    _taskState.value = extractTaskStateUseCase(updatedHistory, _taskState.value, openAiChatRepo)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Ошибка соединения"
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun sendOfflineStreaming(history: List<Message>) {
        if (_modelState.value !is ModelState.Ready) {
            _error.value = "Модель не готова. Дождитесь загрузки и инициализации."
            _isLoading.value = false
            return
        }
        // Добавляем пустое сообщение ассистента, которое будет заполняться токенами
        val assistantMsg = Message(role = "assistant", content = "")
        _messages.value = history + assistantMsg

        try {
            val accumulated = StringBuilder()
            sendMessageUseCase.invokeStreaming(
                messages = history,
                chatRepository = onDeviceChatRepo,
                taskState = _taskState.value
            ).collect { token ->
                accumulated.append(token)
                val updated = (_messages.value.dropLast(1) + assistantMsg.copy(content = accumulated.toString()))
                    .takeLast(MAX_HISTORY)
                _messages.value = updated
            }
            val finalHistory = _messages.value
            // ExtractTaskState в фоне — не блокируем UI
            viewModelScope.launch {
                try {
                    _taskState.value = extractTaskStateUseCase(finalHistory, _taskState.value, onDeviceChatRepo)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Ошибка генерации"
        } finally {
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        onDeviceLlmService.release()
    }
}
