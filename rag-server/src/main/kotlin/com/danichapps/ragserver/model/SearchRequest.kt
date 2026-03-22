package com.danichapps.ragserver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SearchRequest(
    val query: String,
    @JsonProperty("top_k")
    val topK: Int = 3
)
