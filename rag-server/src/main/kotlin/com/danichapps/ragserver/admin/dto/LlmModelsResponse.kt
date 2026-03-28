package com.danichapps.ragserver.admin.dto

import com.danichapps.ragserver.llm.dto.LlmModelInfo

data class LlmModelsResponse(
    val models: List<LlmModelInfo>,
    val active: String?
)
