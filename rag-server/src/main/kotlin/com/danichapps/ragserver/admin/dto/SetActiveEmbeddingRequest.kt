package com.danichapps.ragserver.admin.dto

import jakarta.validation.constraints.NotBlank

data class SetActiveEmbeddingRequest(
    @field:NotBlank
    val modelId: String,
    val confirmReindex: Boolean = false
)
