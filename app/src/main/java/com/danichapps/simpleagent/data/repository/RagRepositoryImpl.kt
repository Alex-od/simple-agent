package com.danichapps.simpleagent.data.repository

import com.danichapps.simpleagent.data.remote.RagService
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.repository.RagRepository

class RagRepositoryImpl(private val ragService: RagService) : RagRepository {

    override suspend fun searchContext(query: String, topK: Int): List<RagChunk> =
        ragService.search(query, topK)
}
