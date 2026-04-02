package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.FilesRequestDto
import com.danichapps.simpleagent.data.remote.dto.FilesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val FILES_BASE_URL = "http://$SERVER_HOST:8100"

class FilesService(private val client: HttpClient) {

    suspend fun analyze(task: String): FilesResponseDto =
        client.post("$FILES_BASE_URL/api/v1/files/analyze") {
            contentType(ContentType.Application.Json)
            setBody(FilesRequestDto(task = task))
        }.body()
}
