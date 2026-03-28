package com.danichapps.ragserver.admin.dto

import jakarta.validation.constraints.NotBlank

data class SetDocumentsPathRequest(
    @field:NotBlank
    val path: String
)
