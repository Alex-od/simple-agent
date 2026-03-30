package com.danichapps.ragserver.project.dto

data class ProjectContextResponse(
    val branch: String?,
    val changedFiles: List<String>,
    val gitRepository: Boolean,
    val projectRoot: String
)
