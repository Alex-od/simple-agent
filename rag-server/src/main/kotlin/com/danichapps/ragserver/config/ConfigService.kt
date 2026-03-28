package com.danichapps.ragserver.config

import com.danichapps.ragserver.config.persistence.DynamicConfigEntity
import com.danichapps.ragserver.config.persistence.DynamicConfigRepository
import org.springframework.stereotype.Service

@Service
class ConfigService(
    private val repository: DynamicConfigRepository
) {

    companion object {
        const val LLM_ACTIVE_MODEL = "LLM_ACTIVE_MODEL"
        const val EMBEDDING_ACTIVE_MODEL = "EMBEDDING_ACTIVE_MODEL"
        const val RAG_DOCUMENTS_PATH = "RAG_DOCUMENTS_PATH"
        const val GGUF_SCAN_DIR = "GGUF_SCAN_DIR"
        const val EMBEDDING_SCAN_DIR = "EMBEDDING_SCAN_DIR"
    }

    fun getActiveLlmModel(): String? = getValue(LLM_ACTIVE_MODEL)

    fun setActiveLlmModel(name: String) = setValue(LLM_ACTIVE_MODEL, name)

    fun getActiveEmbeddingModel(): String? = getValue(EMBEDDING_ACTIVE_MODEL)

    fun setActiveEmbeddingModel(name: String) = setValue(EMBEDDING_ACTIVE_MODEL, name)

    fun getDocumentsPath(): String? = getValue(RAG_DOCUMENTS_PATH)

    fun setDocumentsPath(path: String) = setValue(RAG_DOCUMENTS_PATH, path)

    fun getGgufScanDir(): String? = getValue(GGUF_SCAN_DIR)

    fun setGgufScanDir(path: String) = setValue(GGUF_SCAN_DIR, path)

    fun getEmbeddingScanDir(): String? = getValue(EMBEDDING_SCAN_DIR)

    fun setEmbeddingScanDir(path: String) = setValue(EMBEDDING_SCAN_DIR, path)

    private fun getValue(key: String): String? =
        repository.findById(key).map { it.value }.orElse(null)

    private fun setValue(key: String, value: String) {
        val entity = repository.findById(key).orElse(null)
        if (entity != null) {
            entity.value = value
            repository.save(entity)
        } else {
            repository.save(DynamicConfigEntity(key = key, value = value))
        }
    }
}
