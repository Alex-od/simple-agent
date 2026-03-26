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
private const val MAX_HISTORY_OFFLINE = 6

sealed class ModelState {
    object NotDownloaded : ModelState()
    data class AskingStorageLocation(val options: List<StorageOption>) : ModelState()
    data class Downloading(val progress: Int, val downloadedMb: Int, val totalMb: Int) : ModelState()
    data class DownloadingEmbedding(val progress: Int, val downloadedMb: Int, val totalMb: Int) : ModelState()
    object Initializing : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ChatViewModel(
    private val openAiChatRepo: ChatRepository,
    private val onDeviceChatRepo: ChatRepository,
    private val onDeviceLlmService: OnDeviceLlmService,
    private val modelDownloadManager: ModelDownloadManager,
    private val onlineSendMessageUseCase: SendMessageUseCase,
    private val offlineSendMessageUseCase: SendMessageUseCase,
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

            // Шаг 1: скачать LLM модель
            var llmSuccess = false
            modelDownloadManager.downloadModel(path).collect { state ->
                when (state) {
                    is DownloadState.Downloading -> _modelState.value =
                        ModelState.Downloading(state.progress, state.downloadedMb, state.totalMb)
                    is DownloadState.Done -> llmSuccess = true
                    is DownloadState.Error -> _modelState.value = ModelState.Error(state.message)
                }
            }
            if (!llmSuccess) return@launch

            // Шаг 2: инициализировать LLM (внутри — автоскачивание embedding если нужно)
            initializeModel()
        }
    }

    private fun initializeModel() {
        viewModelScope.launch {
            _modelState.value = ModelState.Initializing
            try {
                onDeviceLlmService.initialize()
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Ошибка инициализации модели")
                return@launch
            }

            // Если embedding-модель ещё не скачана — скачиваем автоматически
            if (!modelDownloadManager.isEmbeddingModelDownloaded()) {
                downloadEmbeddingModelSilently()
            } else {
                _modelState.value = ModelState.Ready
            }
        }
    }

    private suspend fun downloadEmbeddingModelSilently() {
        modelDownloadManager.downloadEmbeddingModel(modelDownloadManager.selectedStoragePath)
            .collect { state ->
                when (state) {
                    is DownloadState.Downloading -> _modelState.value =
                        ModelState.DownloadingEmbedding(state.progress, state.downloadedMb, state.totalMb)
                    is DownloadState.Done -> _modelState.value = ModelState.Ready
                    is DownloadState.Error -> {
                        // Не блокируем LLM при ошибке embedding
                        _modelState.value = ModelState.Ready
                        _error.value = "Embedding модель не загружена: ${state.message}. RAG в офлайне недоступен."
                    }
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

    private suspend fun sendOffline(history: List<Message>) {
        if (_modelState.value !is ModelState.Ready) {
            _error.value = "Модель не готова. Дождитесь загрузки и инициализации."
            _isLoading.value = false
            return
        }
        try {
            val (answer, sources) = offlineSendMessageUseCase(
                history,
                chatRepository = onDeviceChatRepo,
                ragEnabled = _isRagEnabled.value,
                taskState = _taskState.value
            )
            if (answer.isNotBlank()) {
                val updatedHistory = (history + Message(role = "assistant", content = answer, sources = sources)).takeLast(MAX_HISTORY_OFFLINE)
                _messages.value = updatedHistory
            } else {
                _error.value = "Модель не дала ответа"
                _messages.value = history.dropLast(1)
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
