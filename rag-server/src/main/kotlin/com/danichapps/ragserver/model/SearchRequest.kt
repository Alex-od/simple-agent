package com.danichapps.ragserver.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class SearchRequest(
    @field:NotBlank
    val query: String,
    @JsonProperty("top_k")
    @field:Min(1)
    @field:Max(50)
    val topK: Int = 3
)
