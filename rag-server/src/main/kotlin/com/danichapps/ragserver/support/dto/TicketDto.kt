package com.danichapps.ragserver.support.dto

data class TicketDto(
    val id: String,
    val userId: String,
    val userName: String,
    val status: String,
    val category: String,
    val subject: String,
    val description: String,
    val appVersion: String,
    val createdAt: String
)

data class TicketsFile(val tickets: List<TicketDto>)
