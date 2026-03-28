package com.danichapps.ragserver.admin.dto

data class IndexingRequest(
    val chunkSize: Int = 512,
    val chunkOverlap: Int = 64,
    val forceReindex: Boolean = false
)
