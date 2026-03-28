package com.danichapps.ragserver.llm

import com.danichapps.ragserver.llm.dto.LlmModelInfo
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

    fun chat(model: String, messages: List<Map<String, String>>): String {
        val requestBody = mapOf(
            "model" to model,
            "messages" to messages,
            "stream" to false
        )
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
