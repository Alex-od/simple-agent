package com.danichapps.ragserver.llm

import com.danichapps.ragserver.config.ConfigService
import com.danichapps.ragserver.llm.dto.LlmModelInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class LlmService(
    private val ollamaClient: OllamaClient,
    private val configService: ConfigService,
    @Value("\${gguf.scan-dir}") private val ggufScanDir: String
) {

    private val log = LoggerFactory.getLogger(LlmService::class.java)

    fun listAllModels(): List<LlmModelInfo> {
        val activeName = getActiveName()
        val ollamaModels = ollamaClient.listModels().map { it.copy(isActive = it.id == activeName) }
        val localModels = scanGgufModels(activeName)
        return ollamaModels + localModels
    }

    fun getActiveName(): String? = configService.getActiveLlmModel()

    fun setActive(modelName: String) {
        configService.setActiveLlmModel(modelName)
        log.info("Активная LLM модель установлена: {}", modelName)
    }

    private fun scanGgufModels(activeName: String?): List<LlmModelInfo> {
        val dir = File(ggufScanDir)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val ggufFiles = dir.listFiles { file -> file.extension.equals("gguf", ignoreCase = true) }
            ?: return emptyList()

        return ggufFiles.map { file ->
            LlmModelInfo(
                id = "local:${file.name}",
                name = file.nameWithoutExtension,
                provider = "local",
                sizeBytes = file.length(),
                isActive = "local:${file.name}" == activeName
            )
        }
    }
}
