package com.danichapps.simpleagent.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danichapps.simpleagent.data.AppPreferences
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RoutedChatCommand
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.usecase.CommandRouterUseCase
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
    private val commandRouterUseCase: CommandRouterUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _taskState = MutableStateFlow(TaskState())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRagEnabled = MutableStateFlow(prefs.isRagEnabled)
    val isRagEnabled: StateFlow<Boolean> = _isRagEnabled.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(prefs.isOfflineMode)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _maxTokens = MutableStateFlow<Int?>(null)
    val maxTokens: StateFlow<Int?> = _maxTokens.asStateFlow()

    fun toggleRag(enabled: Boolean) {
        _isRagEnabled.value = enabled
        prefs.isRagEnabled = enabled
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        prefs.isOfflineMode = enabled
    }

    fun setMaxTokens(value: Int?) {
        _maxTokens.value = value
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) {
            return
        }

        viewModelScope.launch {
            val userMsg = Message(role = "user", content = text)
            val historyWithUser = (_messages.value + userMsg).takeLast(MAX_HISTORY)
            _messages.value = historyWithUser
            _isLoading.value = true
            _error.value = null
            val activeChatRepo = if (_isOfflineMode.value) ollamaChatRepo else openAiChatRepo

            try {
                val routedCommand = commandRouterUseCase.execute(
                    rawInput = text,
                    historyWithUser = historyWithUser,
                    ragEnabled = _isRagEnabled.value,
                    taskState = _taskState.value
                )

                Log.d(
                    "qqwe_tag CommandRouter",
                    "route=${routedCommand::class.simpleName} rag=${_isRagEnabled.value} offline=${_isOfflineMode.value}"
                )

                val (answer, sources) = when (routedCommand) {
                    is RoutedChatCommand.Default -> sendMessageUseCase(
                        messages = routedCommand.messages,
                        chatRepository = activeChatRepo,
                        ragEnabled = routedCommand.ragEnabled,
                        taskState = routedCommand.taskState,
                        maxTokens = _maxTokens.value
                    )

                    is RoutedChatCommand.Prepared -> {
                        val answer = activeChatRepo.sendMessages(
                            routedCommand.prompt.messages,
                            maxTokens = _maxTokens.value
                        )
                        answer to routedCommand.prompt.sources
                    }

                    is RoutedChatCommand.DirectResponse -> {
                        routedCommand.content to emptyList()
                    }
                }

                val updatedHistory = (
                    historyWithUser + Message(role = "assistant", content = answer, sources = sources)
                    ).takeLast(MAX_HISTORY)
                _messages.value = updatedHistory

                if (routedCommand is RoutedChatCommand.Default) {
                    try {
                        _taskState.value = extractTaskStateUseCase(updatedHistory, _taskState.value, activeChatRepo)
                    } catch (_: Exception) {
                        // Keep the main chat flow alive even if task memory extraction fails.
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка соединения"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
