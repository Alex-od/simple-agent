package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.MessageDto
import kotlinx.coroutines.flow.Flow

interface ChatService {
    suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean = false): String
    fun sendMessagesStreaming(messages: List<MessageDto>, jsonMode: Boolean = false): Flow<String>
}
