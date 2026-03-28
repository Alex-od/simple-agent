package com.danichapps.ragserver.chat

import com.danichapps.ragserver.chat.dto.ChatCompletionRequest
import com.danichapps.ragserver.chat.dto.ChatCompletionResponse
import com.danichapps.ragserver.chat.dto.ChatConfigResponse
import com.danichapps.ragserver.chat.dto.RagAvailability
import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import com.danichapps.ragserver.model.SearchRequest
import com.danichapps.ragserver.model.SearchResponse
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import com.danichapps.ragserver.service.RagService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val ragService: RagService,
    private val llmService: LlmService,
    private val ollamaClient: OllamaClient,
    private val embeddingService: EmbeddingService
) {

    @PostMapping("/completions")
    fun completions(@RequestBody @Valid request: ChatCompletionRequest): ChatCompletionResponse {
        val ragContext = if (request.useRag) {
            ragService.search(request.message, request.topK)
        } else {
            null
        }

        val messages = mutableListOf<Map<String, String>>()

        val contextText = ragContext?.joinToString("\n\n") { it.text } ?: ""
        if (contextText.isNotBlank()) {
            messages.add(mapOf(
                "role" to "system",
                "content" to "Use the following context to answer the question:\n\n$contextText"
            ))
        }
        messages.add(mapOf("role" to "user", "content" to request.message))

        val modelName = llmService.getActiveName() ?: "llama3.2:3b"
        val answer = ollamaClient.chat(modelName, messages)

        return ChatCompletionResponse(
            answer = answer,
            ragContext = ragContext,
            model = modelName
        )
    }

    @PostMapping("/rag/search")
    fun ragSearch(@RequestBody @Valid request: SearchRequest): SearchResponse {
        val results = ragService.search(request.query, request.topK)
        return SearchResponse(results = results)
    }

    @GetMapping("/config")
    fun getConfig(): ChatConfigResponse {
        val chunkCount = ragService.getChunkCount()
        return ChatConfigResponse(
            activeLlm = llmService.getActiveName(),
            activeEmbedding = embeddingService.getModelName(),
            rag = RagAvailability(
                available = chunkCount > 0,
                indexedChunks = chunkCount
            )
        )
    }
}
