package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.ModelsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class LocalServerProbe(
    private val client: HttpClient
) {
    suspend fun checkHealth(baseUrl: String) {
        val rootUrl = baseUrl.toServerRootUrl()
        runCatching { client.get("$rootUrl/health") }
            .getOrElse { throw IllegalStateException("Local server is unreachable: $rootUrl", it) }
    }

    suspend fun fetchModels(baseUrl: String): List<String> {
        val rootUrl = baseUrl.toServerRootUrl()
        return runCatching {
            client.get("$rootUrl/v1/models").body<ModelsResponse>().data.map { it.id }
        }.getOrElse {
            throw IllegalStateException("Failed to fetch models from local server: $rootUrl", it)
        }
    }

    suspend fun ensureModelAvailable(baseUrl: String, model: String) {
        val rootUrl = baseUrl.toServerRootUrl()
        val available = fetchModels(rootUrl)
        check(available.isNotEmpty()) { "Local server returned no models from /v1/models" }
        check(model in available) {
            "Model '$model' is not available on local server. Available: ${available.joinToString()}"
        }
    }

    private fun String.toServerRootUrl(): String {
        val normalized = trim().trimEnd('/')
        return normalized.removeSuffix("/v1")
    }
}
