package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FilesRequestDto(
    val task: String
)

@Serializable
data class FilesResponseDto(
    val result: String,
    val operationLog: List<String> = emptyList(),
    val model: String = ""
)
