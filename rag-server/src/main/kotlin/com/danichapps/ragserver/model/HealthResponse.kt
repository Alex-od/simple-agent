package com.danichapps.ragserver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class HealthResponse(
    val status: String,
    @JsonProperty("indexed_files")
    val indexedFiles: Int,
    @JsonProperty("total_chunks")
    val totalChunks: Int,
    @JsonProperty("embedding_model")
    val embeddingModel: String
)
