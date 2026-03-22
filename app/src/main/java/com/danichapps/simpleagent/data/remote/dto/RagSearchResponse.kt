package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RagResult(
    val text: String,
    val score: Float,
    @SerialName("source_file") val sourceFile: String,
    @SerialName("chunk_index") val chunkIndex: Int
)

@Serializable
data class RagSearchResponse(
    val results: List<RagResult>
)
