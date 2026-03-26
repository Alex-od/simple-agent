package com.danichapps.simpleagent.data.local

import com.danichapps.simpleagent.domain.model.RagChunk

private const val POST_RANK_K = 2
private const val SIMILARITY_THRESHOLD = 0.2f

class GemmaRerankService {

    fun rerank(query: String, candidates: List<ScoredChunk>): List<RagChunk> {
        val filtered = candidates.filter { it.similarity > SIMILARITY_THRESHOLD }
        if (filtered.isEmpty()) return emptyList()
        return filtered.sortedByDescending { it.similarity }.take(POST_RANK_K).map { it.chunk }
    }
}
