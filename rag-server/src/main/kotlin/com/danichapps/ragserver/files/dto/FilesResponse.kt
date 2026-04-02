package com.danichapps.ragserver.files.dto

data class FilesResponse(
    val result: String,
    val operationLog: List<String>,
    val model: String
)
