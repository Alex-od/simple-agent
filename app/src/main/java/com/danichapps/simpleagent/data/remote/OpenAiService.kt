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

private const val BASE_URL = "https://api.openai.com/v1"
private const val MODEL = "gpt-4o-mini"

class OpenAiService(private val client: HttpClient) : ChatService {

    override suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean, maxTokens: Int?): String {
        val response: ChatResponse = client.post("$BASE_URL/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = MODEL,
                messages = messages,
                responseFormat = if (jsonMode) ResponseFormat("json_object") else null,
                maxTokens = maxTokens
            ))
        }.body()
        return response.choices.first().message.content
    }
}
