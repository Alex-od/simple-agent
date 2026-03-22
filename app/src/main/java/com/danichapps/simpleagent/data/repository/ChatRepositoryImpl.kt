package com.danichapps.simpleagent.data.repository

import com.danichapps.simpleagent.data.remote.OpenAiService
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val service: OpenAiService
) : ChatRepository {

    override suspend fun sendMessages(messages: List<Message>, jsonMode: Boolean): String {
        val dtos = messages.map { MessageDto(role = it.role, content = it.content) }
        return service.sendMessages(dtos, jsonMode)
    }
}
