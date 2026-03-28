package com.danichapps.ragserver.llm.dto

data class LlmModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val sizeBytes: Long?,
    val isActive: Boolean
)
