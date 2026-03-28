package com.danichapps.ragserver.admin.dto

data class DocumentsPathResponse(
    val path: String?,
    val fileCount: Int,
    val totalSizeBytes: Long
)
