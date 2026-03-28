package com.danichapps.ragserver.rag.embedding

import com.danichapps.ragserver.common.exception.ApiException
import com.danichapps.ragserver.config.ConfigService
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

@Service
class EmbeddingService(
    private val configService: ConfigService,
    @Value("\${embedding.scan-dir:}") private val defaultScanDir: String
) {

    private val log = LoggerFactory.getLogger(EmbeddingService::class.java)

    private val embeddingModel = AtomicReference(AllMiniLmL6V2QuantizedEmbeddingModel())
    private val currentModelName = AtomicReference("all-minilm-l6-v2")

    fun embed(text: String): Embedding {
        return embeddingModel.get().embed(text).content()
    }

    fun embed(segment: dev.langchain4j.data.segment.TextSegment): Embedding {
        return embeddingModel.get().embed(segment).content()
    }

    fun getModelName(): String = currentModelName.get()

    fun getAvailableModels(): List<String> = listOf("all-minilm-l6-v2")

    fun getModelsPath(): String = configService.getEmbeddingScanDir()
        ?: defaultScanDir.ifBlank { System.getProperty("user.home") + "/.simpleagent/embedding" }

    fun setModelsPath(path: String) {
        configService.setEmbeddingScanDir(path)
        log.info("Путь к embedding моделям установлен: {}", path)
    }

    fun switchModel(modelName: String, confirmReindex: Boolean) {
        if (!confirmReindex) {
            throw ApiException.ConflictException(
                "Switching embedding model requires reindexing. Pass confirmReindex=true to proceed."
            )
        }
        log.info("Переключение embedding модели на: {}", modelName)
        currentModelName.set(modelName)
    }
}
