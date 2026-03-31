package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewResponseDto(
    val summary: String,
    val bugs: List<ReviewFindingDto> = emptyList(),
    val architecturalIssues: List<ReviewFindingDto> = emptyList(),
    val recommendations: List<ReviewFindingDto> = emptyList(),
    val branch: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String = ""
)

@Serializable
data class ReviewFindingDto(
    val severity: String,
    val file: String? = null,
    val line: String? = null,
    val description: String
)
