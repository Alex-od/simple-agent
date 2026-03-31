package com.danichapps.ragserver.review.dto

data class PrReviewRequest(
    val baseBranch: String = "master",
    val diff: String? = null,
    val changedFiles: List<String>? = null,
    val prTitle: String? = null,
    val useRag: Boolean = true,
    val topK: Int = 5
)
