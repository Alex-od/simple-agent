package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SupportResponseDto(
    val answer: String,
    val ticketId: String? = null,
    val ticketSubject: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String = ""
)

@Serializable
data class TicketSummaryDto(
    val id: String,
    val subject: String,
    val status: String,
    val userName: String,
    val category: String
)
