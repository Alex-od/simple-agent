package com.danichapps.ragserver.rag.indexing

enum class IndexingStatus { IDLE, IN_PROGRESS, COMPLETED, FAILED }

data class IndexingState(
    val status: IndexingStatus = IndexingStatus.IDLE,
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val currentFile: String? = null,
    val totalChunks: Int = 0,
    val error: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null
)
