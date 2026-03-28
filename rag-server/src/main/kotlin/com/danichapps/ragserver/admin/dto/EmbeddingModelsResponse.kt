package com.danichapps.ragserver.admin.dto

data class EmbeddingModelsResponse(
    val models: List<String>,
    val active: String?
)
