package com.danichapps.simpleagent.data.local

import com.danichapps.simpleagent.domain.model.RagChunk

private const val DEFAULT_POST_RANK_K = 2
private const val SIMILARITY_THRESHOLD = 0.15f

class GemmaRerankService {

    fun rerank(query: String, candidates: List<ScoredChunk>, topK: Int = DEFAULT_POST_RANK_K): List<RagChunk> {
        val filtered = candidates.filter { it.similarity > SIMILARITY_THRESHOLD }
        if (filtered.isEmpty()) return emptyList()
        return filtered.sortedByDescending { it.similarity }.take(topK.coerceAtLeast(1)).map { it.chunk }
    }
}
