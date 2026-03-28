package com.danichapps.ragserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rag.files")
data class RagFilesProperties(
    val dir: String = "../rag_files",
    val allowedBasePaths: List<String> = listOf(".")
)
