package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SupportRequestDto(
    val question: String,
    val ticketId: String? = null,
    val topK: Int = 3
)
