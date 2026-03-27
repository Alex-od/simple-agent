package com.danichapps.simpleagent.domain.repository

import com.danichapps.simpleagent.domain.model.RagChunk

interface RagRepository {
    suspend fun searchContext(query: String, topK: Int = 3): List<RagChunk>
    suspend fun buildIndexIfNeeded() {}
    fun isIndexed(): Boolean = false
    fun hasDocumentCache(): Boolean = false
}
