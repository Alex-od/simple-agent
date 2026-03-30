package com.danichapps.simpleagent.domain.model

data class PreparedPrompt(
    val messages: List<Message>,
    val sources: List<RagSource>
)
