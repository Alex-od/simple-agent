package com.danichapps.simpleagent.data.repository

import com.danichapps.simpleagent.data.remote.ChatService
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val service: ChatService
) : ChatRepository {

    override suspend fun sendMessages(messages: List<Message>, jsonMode: Boolean, settings: ChatTuningSettings): String {
        val dtos = messages.map { MessageDto(role = it.role, content = it.content) }
        return service.sendMessages(dtos, jsonMode, settings)
    }

    override fun sendMessagesStreaming(messages: List<Message>, jsonMode: Boolean, settings: ChatTuningSettings): Flow<String> {
        val dtos = messages.map { MessageDto(role = it.role, content = it.content) }
        return service.sendMessagesStreaming(dtos, jsonMode, settings)
    }
}
