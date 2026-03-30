package com.danichapps.simpleagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectContextResponse(
    val branch: String? = null,
    val changedFiles: List<String> = emptyList(),
    val gitRepository: Boolean = false,
    val projectRoot: String = ""
)
