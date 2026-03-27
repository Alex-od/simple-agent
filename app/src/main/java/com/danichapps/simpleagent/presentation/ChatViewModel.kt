package com.danichapps.simpleagent.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danichapps.simpleagent.data.local.ChatModeStore
import com.danichapps.simpleagent.data.local.ChatTuningSettingsStore
import com.danichapps.simpleagent.data.local.EmbeddingModelSelectionManager
import com.danichapps.simpleagent.data.local.LocalServerSettingsStore
import com.danichapps.simpleagent.data.local.ModelSelectionManager
import com.danichapps.simpleagent.data.local.RagFolderPreferences
import com.danichapps.simpleagent.data.remote.LlamaCppEmbeddingService
import com.danichapps.simpleagent.data.remote.LocalServerProbe
import com.danichapps.simpleagent.data.remote.OnDeviceLlamaCppService
import com.danichapps.simpleagent.domain.model.ChatMode
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.LocalServerSettings
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
    private val localServerChatRepo: ChatRepository,
    private val onDeviceLlamaCppService: OnDeviceLlamaCppService,
    private val localServerProbe: LocalServerProbe,
    private val llamaCppEmbeddingService: LlamaCppEmbeddingService,
    private val onlineSendMessageUseCase: SendMessageUseCase,
    private val offlineSendMessageUseCase: OfflineSendMessageUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase,
    private val localRagRepository: RagRepository,
    private val ragFolderPreferences: RagFolderPreferences,
    private val modelSelectionManager: ModelSelectionManager,
    private val embeddingModelSelectionManager: EmbeddingModelSelectionManager,
    private val chatTuningSettingsStore: ChatTuningSettingsStore,
    private val chatModeStore: ChatModeStore,
    private val localServerSettingsStore: LocalServerSettingsStore
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

    private val _chatMode = MutableStateFlow(chatModeStore.load())
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(initialModelState(_chatMode.value))
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _chatTuningSettings = MutableStateFlow(chatTuningSettingsStore.load())
    val chatTuningSettings: StateFlow<ChatTuningSettings> = _chatTuningSettings.asStateFlow()

    private val _localServerSettings = MutableStateFlow(localServerSettingsStore.load())
    val localServerSettings: StateFlow<LocalServerSettings> = _localServerSettings.asStateFlow()

    init {
        if (_chatMode.value == ChatMode.LOCAL_SERVER) {
            viewModelScope.launch { initializeLocalServer() }
        } else if (_chatMode.value == ChatMode.ON_DEVICE) {
            viewModelScope.launch { initializeOnDevice() }
        }
    }

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

    fun updateServerBaseUrl(value: String) {
        val updated = _localServerSettings.value.copy(baseUrl = value)
        _localServerSettings.value = updated
        localServerSettingsStore.save(updated)
        if (_chatMode.value == ChatMode.LOCAL_SERVER) {
            _modelState.value = ModelState.NotReady
        }
    }

    fun updateServerModel(value: String) {
        val updated = _localServerSettings.value.copy(model = value)
        _localServerSettings.value = updated
        localServerSettingsStore.save(updated)
        if (_chatMode.value == ChatMode.LOCAL_SERVER) {
            _modelState.value = ModelState.NotReady
        }
    }

    fun setChatMode(mode: ChatMode) {
        if (_chatMode.value == mode) return
        _chatMode.value = mode
        chatModeStore.save(mode)
        _error.value = null

        when (mode) {
            ChatMode.OPENAI -> {
                onDeviceLlamaCppService.release()
                _modelState.value = ModelState.NotReady
                _isRagIndexed.value = localRagRepository.isIndexed()
            }

            ChatMode.ON_DEVICE -> {
                viewModelScope.launch { initializeOnDevice() }
            }

            ChatMode.LOCAL_SERVER -> {
                onDeviceLlamaCppService.release()
                viewModelScope.launch { initializeLocalServer() }
            }
        }
    }

    fun toggleRag(enabled: Boolean) {
        _isRagEnabled.value = enabled
    }

    fun setRagFolder(uri: Uri, displayName: String?) {
        ragFolderPreferences.save(uri, displayName)
        _ragFolderName.value = displayName
        _isRagIndexed.value = localRagRepository.isIndexed()
        when (_chatMode.value) {
            ChatMode.ON_DEVICE -> viewModelScope.launch { initializeOnDevice() }
            ChatMode.LOCAL_SERVER -> viewModelScope.launch { initializeLocalServer() }
            ChatMode.OPENAI -> Unit
        }
    }

    fun importModel(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            try {
                _modelState.value = ModelState.Initializing
                onDeviceLlamaCppService.release()
                val importedFile = modelSelectionManager.importModel(uri, displayName)
                _modelFileName.value = displayName ?: importedFile.name
                if (_chatMode.value == ChatMode.ON_DEVICE) {
                    initializeOnDevice()
                } else {
                    _modelState.value = initialModelState(_chatMode.value)
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
                when (_chatMode.value) {
                    ChatMode.ON_DEVICE -> initializeOnDevice()
                    ChatMode.LOCAL_SERVER -> initializeLocalServer()
                    ChatMode.OPENAI -> {
                        _modelState.value = initialModelState(_chatMode.value)
                        _error.value = "Embedding-модель выбрана: ${importedFile.name}"
                    }
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Не удалось импортировать embedding-модель")
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
            _isLoading.value = true
            _error.value = null

            val ready = ensureCurrentModeReady()
            if (!ready) {
                _isLoading.value = false
                return@launch
            }

            val userMsg = Message(role = "user", content = text)
            val maxHistory = if (_chatMode.value == ChatMode.ON_DEVICE) MAX_HISTORY_OFFLINE else MAX_HISTORY
            val historyWithUser = (_messages.value + userMsg).takeLast(maxHistory)
            _messages.value = historyWithUser

            when (_chatMode.value) {
                ChatMode.ON_DEVICE -> sendOnDevice(historyWithUser)
                ChatMode.OPENAI -> sendRemote(historyWithUser, openAiChatRepo)
                ChatMode.LOCAL_SERVER -> sendRemote(historyWithUser, localServerChatRepo)
            }
        }
    }

    private suspend fun ensureCurrentModeReady(): Boolean = when (_chatMode.value) {
        ChatMode.OPENAI -> true
        ChatMode.ON_DEVICE -> initializeOnDevice()
        ChatMode.LOCAL_SERVER -> initializeLocalServer()
    }

    private suspend fun initializeOnDevice(): Boolean {
        _modelState.value = ModelState.Initializing
        return try {
            onDeviceLlamaCppService.initialize()
            prepareRagIfNeeded()
            _modelState.value = ModelState.Ready
            true
        } catch (e: Exception) {
            _modelState.value = ModelState.Error(
                e.message ?: "Не удалось инициализировать on-device llama.cpp"
            )
            false
        }
    }

    private suspend fun initializeLocalServer(): Boolean {
        _modelState.value = ModelState.Initializing
        return try {
            val settings = _localServerSettings.value
            check(settings.baseUrl.isNotBlank()) { "Укажите Local server URL" }
            check(settings.model.isNotBlank()) { "Укажите model name для Local server" }
            localServerProbe.checkHealth(settings.baseUrl)
            localServerProbe.ensureModelAvailable(settings.baseUrl, settings.model)
            prepareRagIfNeeded()
            _modelState.value = ModelState.Ready
            true
        } catch (e: Exception) {
            _modelState.value = ModelState.Error(
                e.message ?: "Не удалось проверить Local server"
            )
            false
        }
    }

    private suspend fun prepareRagIfNeeded() {
        if (ragFolderPreferences.hasFolder() && !localRagRepository.isIndexed()) {
            _modelState.value = ModelState.Indexing
            localRagRepository.buildIndexIfNeeded()
        }
        _isRagIndexed.value = localRagRepository.isIndexed()
    }

    private suspend fun sendRemote(history: List<Message>, chatRepository: ChatRepository) {
        try {
            val (answer, sources) = onlineSendMessageUseCase(
                history,
                chatRepository = chatRepository,
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
            _error.value = when (_chatMode.value) {
                ChatMode.OPENAI -> e.message ?: "Ошибка соединения с OpenAI"
                ChatMode.LOCAL_SERVER -> e.message ?: "Ошибка Local server"
                ChatMode.ON_DEVICE -> e.message ?: "Ошибка удалённой генерации"
            }
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun sendOnDevice(history: List<Message>) {
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

    private fun initialModelState(mode: ChatMode): ModelState =
        if (mode == ChatMode.OPENAI) ModelState.NotReady else ModelState.NotReady
}
