package com.danichapps.ragserver.controller

import com.danichapps.ragserver.model.HealthResponse
import com.danichapps.ragserver.model.SearchRequest
import com.danichapps.ragserver.model.SearchResponse
import com.danichapps.ragserver.service.RagService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RagController(
    private val ragService: RagService
) {

    @PostMapping("/search")
    fun search(@RequestBody request: SearchRequest): SearchResponse {
        val results = ragService.search(request.query, request.topK)
        return SearchResponse(results = results)
    }

    @GetMapping("/health")
    fun health(): HealthResponse {
        return HealthResponse(
            status = "ok",
            indexedFiles = ragService.getIndexedFilesCount(),
            totalChunks = ragService.getTotalChunksCount(),
            embeddingModel = RagService.EMBEDDING_MODEL_NAME
        )
    }
}
