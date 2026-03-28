package com.danichapps.ragserver.common.dto

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = Instant.now().toString()
)
