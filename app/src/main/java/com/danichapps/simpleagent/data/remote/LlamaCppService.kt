package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ChatRequest
import com.danichapps.simpleagent.data.remote.dto.ChatResponse
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.data.remote.dto.ModelsResponse
import com.danichapps.simpleagent.data.remote.dto.ResponseFormat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LlamaCppService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val model: String
) : ChatService {

    suspend fun initialize() {
        runCatching { client.get("$baseUrl/health") }
        val models = client.get("$baseUrl/v1/models").body<ModelsResponse>()
        val availableModels = models.data.map { it.id }
        if (availableModels.isEmpty()) {
            error("llama.cpp сервер не вернул список моделей по /v1/models")
        }
        if (model !in availableModels) {
            error(
                "Модель '$model' не найдена на llama.cpp сервере. Доступно: ${availableModels.joinToString()}"
            )
        }
    }

    fun release() = Unit

    override suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean): String {
        val response: ChatResponse = client.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model,
                    messages = messages,
                    responseFormat = if (jsonMode) ResponseFormat("json_object") else null
                )
            )
        }.body()
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    override fun sendMessagesStreaming(messages: List<MessageDto>, jsonMode: Boolean): Flow<String> = flow {
        emit(sendMessages(messages, jsonMode))
    }
}
