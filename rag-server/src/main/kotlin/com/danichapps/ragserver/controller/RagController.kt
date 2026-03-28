package com.danichapps.ragserver.controller

import com.danichapps.ragserver.model.HealthResponse
import com.danichapps.ragserver.service.RagService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RagController(
    private val ragService: RagService
) {

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
