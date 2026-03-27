package com.danichapps.simpleagent.domain.model

data class ChatTuningSettings(
    val temperature: Float = 0.2f,
    val maxTokens: Int = 64,
    val systemPrompt: String = ""
)
