package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RagSearchRequest(
    val query: String,
    @SerialName("top_k") val topK: Int = 3
)
