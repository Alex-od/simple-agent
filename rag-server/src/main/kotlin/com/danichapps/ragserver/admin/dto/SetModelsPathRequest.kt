package com.danichapps.ragserver.admin.dto

import jakarta.validation.constraints.NotBlank

data class SetModelsPathRequest(
    @field:NotBlank
    val path: String
)
