package com.danichapps.simpleagent.domain.model

data class ChatTuningSettings(
    val temperature: Float = 0.2f,
    val maxTokens: Int = 96,
    val systemPrompt: String = ""
)
