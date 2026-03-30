package com.danichapps.simpleagent.domain.model

data class HelpCommandContext(
    val question: String,
    val ragChunks: List<RagChunk>,
    val projectContext: ProjectContext?
)
