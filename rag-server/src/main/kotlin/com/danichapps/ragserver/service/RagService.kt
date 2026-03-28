package com.danichapps.ragserver.service

import com.danichapps.ragserver.model.SearchResult
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import jakarta.annotation.PostConstruct
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream

@Service
class RagService(
    @Value("\${rag.files.dir}") private val filesDir: String,
    @Value("\${rag.auto-index:false}") private val autoIndex: Boolean,
    private val embeddingService: EmbeddingService
) {

    private val log = LoggerFactory.getLogger(RagService::class.java)

    private val embeddingStore = InMemoryEmbeddingStore<TextSegment>()

    private var indexedFilesCount = 0
    private var totalChunksCount = 0

    companion object {
        private const val CHUNK_SIZE_WORDS = 500
        private const val OVERLAP_WORDS = 100
        const val EMBEDDING_MODEL_NAME = "all-minilm-l6-v2"
    }

    @PostConstruct
    fun init() {
        if (!autoIndex) {
            log.info("Автоматическая индексация отключена (rag.auto-index=false)")
            return
        }
        val dir = File(filesDir)
        indexDirectory(dir)
    }

    fun indexDirectory(dir: File): IndexingCallback {
        val callback = IndexingCallback()

        if (!dir.exists() || !dir.isDirectory) {
            log.warn("Папка RAG-файлов не найдена: {}. Индексация пропущена.", dir.absolutePath)
            return callback
        }

        val docxFiles = dir.listFiles { file -> file.extension.equals("docx", ignoreCase = true) } ?: emptyArray()

        if (docxFiles.isEmpty()) {
            log.warn("Файлы .docx не найдены в {}", dir.absolutePath)
            return callback
        }

        callback.totalFiles = docxFiles.size
        log.info("Найдено {} .docx файл(ов) в {}. Начинаю индексацию...", docxFiles.size, dir.absolutePath)

        for (file in docxFiles) {
            try {
                callback.currentFile = file.name
                indexFile(file)
                indexedFilesCount++
                callback.processedFiles++
            } catch (e: Exception) {
                log.error("Ошибка индексации файла: {}", file.name, e)
            }
        }

        callback.totalChunks = totalChunksCount
        log.info("Индексация завершена. Файлов: {}, чанков: {}", indexedFilesCount, totalChunksCount)
        return callback
    }

    fun clearStore() {
        embeddingStore.removeAll()
        indexedFilesCount = 0
        totalChunksCount = 0
        log.info("Embedding store очищен")
    }

    fun getChunkCount(): Int = totalChunksCount

    private fun indexFile(file: File) {
        log.info("Индексирую файл: {}", file.name)
        val text = extractText(file)
        if (text.isBlank()) {
            log.warn("Из файла {} не извлечён текст", file.name)
            return
        }

        val chunks = chunkText(text)
        log.info("Файл {}: извлечено {} чанков", file.name, chunks.size)

        for ((index, chunk) in chunks.withIndex()) {
            val segment = TextSegment.from(
                chunk,
                Metadata.from(mapOf("source_file" to file.name, "chunk_index" to index.toString()))
            )
            val embedding: Embedding = embeddingService.embed(segment)
            embeddingStore.add(embedding, segment)
            totalChunksCount++
        }
    }

    private fun extractText(file: File): String {
        val sb = StringBuilder()
        FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { doc ->
                for (paragraph in doc.paragraphs) {
                    val text = paragraph.text.trim()
                    if (text.isNotEmpty()) sb.appendLine(text)
                }
                for (table in doc.tables) {
                    for (row in table.rows) {
                        val rowText = row.tableCells.joinToString(" | ") { it.text.trim() }
                        if (rowText.isNotBlank()) sb.appendLine(rowText)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun chunkText(text: String): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= CHUNK_SIZE_WORDS) return listOf(words.joinToString(" "))

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            val end = minOf(start + CHUNK_SIZE_WORDS, words.size)
            chunks.add(words.subList(start, end).joinToString(" "))
            start += CHUNK_SIZE_WORDS - OVERLAP_WORDS
        }
        return chunks
    }

    fun search(query: String, topK: Int): List<SearchResult> {
        return try {
            val queryEmbedding: Embedding = embeddingService.embed(query)
            val request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build()
            val matches = embeddingStore.search(request).matches()
            matches.map { match ->
                val segment = match.embedded()
                SearchResult(
                    text = segment.text(),
                    score = match.score(),
                    sourceFile = segment.metadata().getString("source_file") ?: "unknown",
                    chunkIndex = segment.metadata().getString("chunk_index")?.toIntOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            log.error("Ошибка поиска, queryLength={}", query.length, e)
            emptyList()
        }
    }

    fun getIndexedFilesCount(): Int = indexedFilesCount
    fun getTotalChunksCount(): Int = totalChunksCount

    data class IndexingCallback(
        var totalFiles: Int = 0,
        var processedFiles: Int = 0,
        var currentFile: String? = null,
        var totalChunks: Int = 0
    )
}
