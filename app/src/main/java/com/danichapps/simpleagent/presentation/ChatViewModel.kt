package com.danichapps.simpleagent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class ChatViewModel(
    private val openAiChatRepo: ChatRepository,
    private val ollamaChatRepo: ChatRepository,
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

    fun toggleRag(enabled: Boolean) {
        _isRagEnabled.value = enabled
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        viewModelScope.launch {
            val userMsg = Message(role = "user", content = text)
            val historyWithUser = (_messages.value + userMsg).takeLast(MAX_HISTORY)
            _messages.value = historyWithUser
            _isLoading.value = true
            _error.value = null
            val activeChatRepo = if (_isOfflineMode.value) ollamaChatRepo else openAiChatRepo
            try {
                val (answer, sources) = sendMessageUseCase(
                    historyWithUser,
                    chatRepository = activeChatRepo,
                    ragEnabled = _isRagEnabled.value,
                    taskState = _taskState.value
                )
                val updatedHistory = (historyWithUser + Message(role = "assistant", content = answer, sources = sources))
                    .takeLast(MAX_HISTORY)
                _messages.value = updatedHistory
                try {
                    _taskState.value = extractTaskStateUseCase(updatedHistory, _taskState.value, activeChatRepo)
                } catch (e: Exception) {
                    // не мешаем основному флоу
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка соединения"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
