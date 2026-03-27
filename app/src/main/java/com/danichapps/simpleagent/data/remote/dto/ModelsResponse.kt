package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String
)
