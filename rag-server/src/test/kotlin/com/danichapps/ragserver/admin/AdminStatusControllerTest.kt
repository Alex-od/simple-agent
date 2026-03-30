package com.danichapps.ragserver.admin

import com.danichapps.ragserver.config.ConfigService
import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import com.danichapps.ragserver.rag.indexing.IndexingService
import com.danichapps.ragserver.rag.indexing.IndexingState
import com.danichapps.ragserver.rag.indexing.IndexingStatus
import com.danichapps.ragserver.service.RagService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdminStatusControllerTest {

    private val llmService = mockk<LlmService>()
    private val embeddingService = mockk<EmbeddingService>()
    private val configService = mockk<ConfigService>()
    private val indexingService = mockk<IndexingService>()
    private val ragService = mockk<RagService>()

    private val controller = AdminStatusController(
        llmService = llmService,
        embeddingService = embeddingService,
        configService = configService,
        indexingService = indexingService,
        ragService = ragService,
        qdrantHost = "localhost",
        qdrantPort = 6334,
        qdrantCollection = "rag-documents"
    )

    @Test
    fun `getStatus returns current indexing and rag status`() {
        every { llmService.getActiveName() } returns "llama3.2:3b"
        every { embeddingService.getModelName() } returns "all-minilm-l6-v2"
        every { configService.getDocumentsPath() } returns "C:/StudioProjects/SimpleAgent"
        every { indexingService.getState() } returns IndexingState(
            status = IndexingStatus.COMPLETED,
            totalFiles = 5,
            processedFiles = 5,
            totalChunks = 29,
            startedAt = "2026-03-30T19:06:34Z",
            completedAt = "2026-03-30T19:06:37Z"
        )
        every { ragService.getRealChunkCount() } returns 29L
        every { ragService.isQdrantConnected() } returns true

        val response = controller.getStatus()

        assertEquals("0.0.1-SNAPSHOT", response.serverVersion)
        assertEquals("llama3.2:3b", response.activeLlm)
        assertEquals("all-minilm-l6-v2", response.activeEmbedding)
        assertEquals("C:/StudioProjects/SimpleAgent", response.rag.documentsPath)
        assertEquals(29L, response.rag.indexedChunks)
        assertEquals("COMPLETED", response.rag.indexingStatus)
        assertEquals("rag-documents", response.rag.qdrantCollection)
        assertEquals("localhost:6334", response.rag.qdrantEndpoint)
        assertTrue(response.rag.qdrantConnected)
        assertEquals(5, response.indexing.totalFiles)
        assertEquals(5, response.indexing.processedFiles)
        assertEquals(29, response.indexing.totalChunks)
    }
}
