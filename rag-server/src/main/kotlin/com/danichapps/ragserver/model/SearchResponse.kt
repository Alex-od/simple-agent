package com.danichapps.ragserver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SearchResponse(
    val results: List<SearchResult>
)

data class SearchResult(
    val text: String,
    val score: Double,
    @JsonProperty("source_file")
    val sourceFile: String,
    @JsonProperty("chunk_index")
    val chunkIndex: Int
)
