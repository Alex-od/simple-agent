package com.danichapps.simpleagent.domain.repository

import com.danichapps.simpleagent.domain.model.Message

interface ChatRepository {
    suspend fun sendMessages(messages: List<Message>, jsonMode: Boolean = false, maxTokens: Int? = null): String
}
