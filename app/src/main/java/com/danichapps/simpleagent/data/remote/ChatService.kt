package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import kotlinx.coroutines.flow.Flow

interface ChatService {
    suspend fun sendMessages(
        messages: List<MessageDto>,
        jsonMode: Boolean = false,
        settings: ChatTuningSettings = ChatTuningSettings()
    ): String

    fun sendMessagesStreaming(
        messages: List<MessageDto>,
        jsonMode: Boolean = false,
        settings: ChatTuningSettings = ChatTuningSettings()
    ): Flow<String>
}
