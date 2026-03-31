package com.danichapps.ragserver.review.dto

data class PrReviewResponse(
    val summary: String,
    val bugs: List<String> = emptyList(),
    val architecturalIssues: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val branch: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String
)
