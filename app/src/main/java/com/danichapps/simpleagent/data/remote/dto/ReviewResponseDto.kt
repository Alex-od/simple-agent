package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewResponseDto(
    val summary: String,
    val bugs: List<String> = emptyList(),
    val architecturalIssues: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val branch: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String = ""
)
