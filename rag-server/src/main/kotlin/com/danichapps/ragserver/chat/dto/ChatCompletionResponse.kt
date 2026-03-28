package com.danichapps.ragserver.chat.dto

import com.danichapps.ragserver.model.SearchResult

data class ChatCompletionResponse(
    val answer: String,
    val ragContext: List<SearchResult>?,
    val model: String?
)
