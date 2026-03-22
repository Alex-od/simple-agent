package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: MessageDto
)
