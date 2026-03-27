package com.danichapps.simpleagent.domain.repository

import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessages(
        messages: List<Message>,
        jsonMode: Boolean = false,
        settings: ChatTuningSettings = ChatTuningSettings()
    ): String

    fun sendMessagesStreaming(
        messages: List<Message>,
        jsonMode: Boolean = false,
        settings: ChatTuningSettings = ChatTuningSettings()
    ): Flow<String>
}
