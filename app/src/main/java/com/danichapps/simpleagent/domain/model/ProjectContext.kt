package com.danichapps.simpleagent.domain.model

data class ProjectContext(
    val branch: String?,
    val changedFiles: List<String>,
    val gitRepository: Boolean,
    val projectRoot: String
)
