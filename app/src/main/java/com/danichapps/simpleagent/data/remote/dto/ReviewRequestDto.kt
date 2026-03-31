package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequestDto(
    val baseBranch: String = "master",
    val useRag: Boolean = true
)
