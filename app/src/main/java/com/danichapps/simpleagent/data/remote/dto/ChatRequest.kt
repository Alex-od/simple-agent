package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)
