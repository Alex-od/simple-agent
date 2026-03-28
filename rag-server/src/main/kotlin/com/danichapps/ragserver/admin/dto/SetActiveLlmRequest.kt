package com.danichapps.ragserver.admin.dto

import jakarta.validation.constraints.NotBlank

data class SetActiveLlmRequest(
    @field:NotBlank
    val modelId: String
)
