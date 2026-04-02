package com.danichapps.ragserver.llm.dto

import com.fasterxml.jackson.databind.JsonNode

data class ToolCallResult(
    val name: String,
    val arguments: JsonNode
)

data class ToolCallResponse(
    val content: String,
    val toolCalls: List<ToolCallResult>
) {
    val hasToolCalls: Boolean get() = toolCalls.isNotEmpty()
}
