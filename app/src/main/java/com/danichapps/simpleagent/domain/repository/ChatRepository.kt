package com.danichapps.simpleagent.domain.repository

import com.danichapps.simpleagent.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessages(messages: List<Message>, jsonMode: Boolean = false): String
    fun sendMessagesStreaming(messages: List<Message>, jsonMode: Boolean = false): Flow<String>
}
