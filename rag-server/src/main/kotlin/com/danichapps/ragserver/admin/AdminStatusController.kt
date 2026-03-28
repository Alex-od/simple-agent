package com.danichapps.ragserver.admin

import com.danichapps.ragserver.admin.dto.RagStatus
import com.danichapps.ragserver.admin.dto.SystemStatusResponse
import com.danichapps.ragserver.config.ConfigService
import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import com.danichapps.ragserver.rag.indexing.IndexingService
import com.danichapps.ragserver.service.RagService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminStatusController(
    private val llmService: LlmService,
    private val embeddingService: EmbeddingService,
    private val configService: ConfigService,
    private val indexingService: IndexingService,
    private val ragService: RagService
) {

    @GetMapping("/status")
    fun getStatus(): SystemStatusResponse {
        val indexingState = indexingService.getState()
        return SystemStatusResponse(
            serverVersion = "0.0.1-SNAPSHOT",
            activeLlm = llmService.getActiveName(),
            activeEmbedding = embeddingService.getModelName(),
            rag = RagStatus(
                documentsPath = configService.getDocumentsPath(),
                indexedChunks = ragService.getChunkCount(),
                indexingStatus = indexingState.status.name
            ),
            indexing = indexingState
        )
    }
}
