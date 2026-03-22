package com.danichapps.simpleagent.domain.model

data class Message(
    val role: String,
    val content: String,
    val sources: List<RagSource> = emptyList()
)
