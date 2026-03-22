package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.RagSearchRequest
import com.danichapps.simpleagent.data.remote.dto.RagSearchResponse
import com.danichapps.simpleagent.domain.model.RagChunk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val RAG_BASE_URL = "http://10.0.2.2:8100"

class RagService(private val client: HttpClient) {

    suspend fun search(query: String, topK: Int = 3): List<RagChunk> {
        return try {
            val response: RagSearchResponse = client.post("$RAG_BASE_URL/search") {
                contentType(ContentType.Application.Json)
                setBody(RagSearchRequest(query = query, topK = topK))
            }.body()
            response.results.map { result ->
                RagChunk(
                    source = result.sourceFile,
                    chunkIndex = result.chunkIndex,
                    text = result.text
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
