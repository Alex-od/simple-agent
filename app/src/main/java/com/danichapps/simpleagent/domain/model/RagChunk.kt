package com.danichapps.simpleagent.domain.model

data class RagChunk(
    val source: String,
    val chunkIndex: Int,
    val text: String
)
