package com.danichapps.ragserver.review.dto

data class PrReviewResponse(
    val summary: String,
    val bugs: List<ReviewFinding> = emptyList(),
    val architecturalIssues: List<ReviewFinding> = emptyList(),
    val recommendations: List<ReviewFinding> = emptyList(),
    val branch: String? = null,
    val ragContextUsed: Boolean = false,
    val model: String
)

data class ReviewFinding(
    val severity: String,
    val file: String? = null,
    val line: String? = null,
    val description: String
)
