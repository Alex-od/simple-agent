package com.danichapps.ragserver.llm

import com.danichapps.ragserver.llm.dto.LlmModelInfo
import com.danichapps.ragserver.llm.dto.ToolCallResponse
import com.danichapps.ragserver.llm.dto.ToolCallResult
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class OllamaClient(
    @Value("\${ollama.base-url}") private val baseUrl: String
) {

    private val log = LoggerFactory.getLogger(OllamaClient::class.java)
    private val webClient: WebClient = WebClient.builder().baseUrl(baseUrl).build()

    fun chat(model: String, messages: List<Map<String, String>>, format: String? = null): String {
        val requestBody = buildMap {
            put("model", model)
            put("messages", messages)
            put("stream", false)
            if (format != null) put("format", format)
        }
        val response = webClient.post()
            .uri("/api/chat")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()
        return response?.get("message")?.get("content")?.asText()
            ?: throw IllegalStateException("Пустой ответ от Ollama")
    }

    fun chatWithTools(
        model: String,
        messages: List<Map<String, Any>>,
        tools: List<Map<String, Any>>
    ): ToolCallResponse {
        val requestBody = mapOf(
            "model" to model,
            "messages" to messages,
            "tools" to tools,
            "stream" to false
        )
        val response = webClient.post()
            .uri("/api/chat")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()
            ?: throw IllegalStateException("Пустой ответ от Ollama (tools)")

        val message = response.get("message")
            ?: throw IllegalStateException("Нет поля message в ответе Ollama")

        val content = message.get("content")?.asText().orEmpty()
        val toolCallsNode = message.get("tool_calls")

        val toolCalls = if (toolCallsNode != null && toolCallsNode.isArray && toolCallsNode.size() > 0) {
            toolCallsNode.map { tc ->
                val fn = tc.get("function")
                ToolCallResult(
                    name = fn?.get("name")?.asText() ?: "",
                    arguments = fn?.get("arguments") ?: tc
                )
            }
        } else emptyList()

        log.debug(
            "qqwe_tag OllamaClient, chatWithTools: content='{}', toolCalls={}",
            content.take(80), toolCalls.map { it.name }
        )

        return ToolCallResponse(content = content, toolCalls = toolCalls)
    }

    fun listModels(): List<LlmModelInfo> {
        return try {
            val response = webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .block()

            val models = response?.get("models") ?: return emptyList()
            models.map { node ->
                LlmModelInfo(
                    id = node.get("name")?.asText() ?: "unknown",
                    name = node.get("name")?.asText() ?: "unknown",
                    provider = "ollama",
                    sizeBytes = node.get("size")?.asLong(),
                    isActive = false
                )
            }
        } catch (e: Exception) {
            log.warn("Ollama API недоступен по адресу {}: {}", baseUrl, e.message)
            emptyList()
        }
    }
}
