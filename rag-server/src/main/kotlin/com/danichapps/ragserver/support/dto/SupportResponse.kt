package com.danichapps.ragserver.support.dto

data class SupportResponse(
    val answer: String,
    val ticketId: String? = null,
    val ticketSubject: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String
)
