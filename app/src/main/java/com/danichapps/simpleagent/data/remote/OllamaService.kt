package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ChatRequest
import com.danichapps.simpleagent.data.remote.dto.ChatResponse
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.danichapps.simpleagent.data.remote.dto.ResponseFormat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val OLLAMA_BASE_URL = "http://192.168.0.102:11434/v1"
private const val OLLAMA_MODEL = "llama3.2:3b"

class OllamaService(private val client: HttpClient) : ChatService {

    override suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean): String {
        val response: ChatResponse = client.post("$OLLAMA_BASE_URL/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = OLLAMA_MODEL,
                messages = messages,
                responseFormat = if (jsonMode) ResponseFormat("json_object") else null
            ))
        }.body()
        return response.choices.first().message.content
    }
}
