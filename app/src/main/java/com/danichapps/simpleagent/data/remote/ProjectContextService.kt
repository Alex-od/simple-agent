package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ProjectContextResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val PROJECT_CONTEXT_BASE_URL = "http://$SERVER_HOST:8100"

class ProjectContextService(
    private val client: HttpClient
) {

    suspend fun getProjectContext(): ProjectContextResponse =
        client.get("$PROJECT_CONTEXT_BASE_URL/api/v1/project/context").body()
}
