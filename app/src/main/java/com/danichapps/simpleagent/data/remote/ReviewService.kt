package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ReviewRequestDto
import com.danichapps.simpleagent.data.remote.dto.ReviewResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val RAG_BASE_URL = "http://$SERVER_HOST:8100"

class ReviewService(private val client: HttpClient) {

    suspend fun reviewPr(baseBranch: String = "master"): ReviewResponseDto {
        return client.post("$RAG_BASE_URL/api/v1/review/pr") {
            contentType(ContentType.Application.Json)
            setBody(ReviewRequestDto(baseBranch = baseBranch))
        }.body()
    }
}
