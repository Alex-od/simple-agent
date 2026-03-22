package com.danichapps.simpleagent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskState(
    val goal: String = "",
    val clarifications: List<String> = emptyList(),
    val constraints: List<String> = emptyList()
) {
    fun isEmpty() = goal.isBlank() && clarifications.isEmpty() && constraints.isEmpty()
}
