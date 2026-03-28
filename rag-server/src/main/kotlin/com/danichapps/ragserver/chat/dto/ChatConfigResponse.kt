package com.danichapps.ragserver.chat.dto

data class ChatConfigResponse(
    val activeLlm: String?,
    val activeEmbedding: String?,
    val rag: RagAvailability
)

data class RagAvailability(
    val available: Boolean,
    val indexedChunks: Int
)
