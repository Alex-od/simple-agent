package com.danichapps.ragserver.admin.dto

import com.danichapps.ragserver.rag.indexing.IndexingState

data class SystemStatusResponse(
    val serverVersion: String,
    val activeLlm: String?,
    val activeEmbedding: String?,
    val rag: RagStatus,
    val indexing: IndexingState
)

data class RagStatus(
    val documentsPath: String?,
    val indexedChunks: Long,
    val indexingStatus: String,
    val qdrantConnected: Boolean,
    val qdrantCollection: String,
    val qdrantEndpoint: String
)
