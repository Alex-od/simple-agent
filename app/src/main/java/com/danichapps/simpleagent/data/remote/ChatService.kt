package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.MessageDto

interface ChatService {
    suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean = false): String
}
