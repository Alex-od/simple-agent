package com.danichapps.ragserver.chat.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ChatCompletionRequest(
    @field:NotBlank
    val message: String,
    val useRag: Boolean = true,
    @field:Min(1)
    @field:Max(20)
    val topK: Int = 5
)
