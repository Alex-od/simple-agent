package com.danichapps.simpleagent.domain.model

data class RagSource(
    val source: String,
    val chunkIndex: Int,
    val quote: String
)
