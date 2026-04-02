package com.danichapps.ragserver.support.dto

data class SupportRequest(
    val question: String,
    val ticketId: String? = null,
    val topK: Int = 3
)
