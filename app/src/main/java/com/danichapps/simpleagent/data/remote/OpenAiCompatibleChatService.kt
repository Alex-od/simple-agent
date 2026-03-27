package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ChatRequest
import com.danichapps.simpleagent.data.remote.dto.ChatResponse
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.data.remote.dto.ResponseFormat
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OpenAiCompatibleChatService(
    private val client: HttpClient,
    private val endpointProvider: () -> ChatEndpointConfig
) : ChatService {

    override suspend fun sendMessages(
        messages: List<MessageDto>,
        jsonMode: Boolean,
        settings: ChatTuningSettings
    ): String {
        val endpoint = endpointProvider()
        val response: ChatResponse = client.post("${endpoint.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = endpoint.model,
                    messages = messages,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens,
                    responseFormat = if (jsonMode) ResponseFormat("json_object") else null
                )
            )
        }.body()
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    override fun sendMessagesStreaming(
        messages: List<MessageDto>,
        jsonMode: Boolean,
        settings: ChatTuningSettings
    ): Flow<String> = flow {
        emit(sendMessages(messages, jsonMode, settings))
    }
}
