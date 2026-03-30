package com.danichapps.ragserver.service

import com.danichapps.ragserver.model.SearchResult
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore
import io.qdrant.client.WithPayloadSelectorFactory
import io.qdrant.client.WithVectorsSelectorFactory
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.JsonWithInt
import io.qdrant.client.grpc.Points
import jakarta.annotation.PostConstruct
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

@Service
class RagService(
    @Value("\${rag.files.dir}") private val filesDir: String,
    @Value("\${rag.auto-index:false}") private val autoIndex: Boolean,
    @Value("\${qdrant.collection-name}") private val collectionName: String,
    @Value("\${qdrant.vector-size}") private val vectorSize: Long,
    private val embeddingService: EmbeddingService,
    private val embeddingStore: QdrantEmbeddingStore,
    private val qdrantClient: QdrantClient
) {

    private val log = LoggerFactory.getLogger(RagService::class.java)

    private var indexedFilesCount = 0
    private var totalChunksCount = 0

    companion object {
        private const val CHUNK_SIZE_WORDS = 500
        private const val OVERLAP_WORDS = 100
        private val SUPPORTED_EXTENSIONS = setOf("docx", "md")
        private val IGNORED_DIRECTORIES = setOf(
            ".git",
            ".gradle",
            ".gradle-local",
            ".gradle-local-tmp",
            ".idea",
            ".kotlin",
            "build"
        )
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

        val supportedFiles = collectSupportedFiles(dir)

        if (supportedFiles.isEmpty()) {
            log.warn("Поддерживаемые файлы (.docx, .md) не найдены в {}", dir.absolutePath)
            return callback
        }

        callback.totalFiles = supportedFiles.size
        log.info(
            "Найдено {} поддерживаемых файл(ов) в {}. Начинаю индексацию...",
            supportedFiles.size,
            dir.absolutePath
        )

        for (file in supportedFiles) {
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
        try {
            qdrantClient.deleteCollectionAsync(collectionName).get()
            log.info("Коллекция Qdrant удалена: {}", collectionName)
        } catch (e: Exception) {
            log.info("Коллекция {} не существует, создаю новую", collectionName)
        }
        qdrantClient.createCollectionAsync(
            collectionName,
            VectorParams.newBuilder()
                .setSize(vectorSize)
                .setDistance(Distance.Cosine)
                .build()
        ).get()
        indexedFilesCount = 0
        totalChunksCount = 0
        log.info("Коллекция Qdrant пересоздана (очищена): {}", collectionName)
    }

    fun getChunkCount(): Int = getRealChunkCount().toInt()

    fun getRealChunkCount(): Long {
        return try {
            val info = qdrantClient.getCollectionInfoAsync(collectionName).get()
            when {
                info.hasPointsCount() && info.pointsCount > 0L -> info.pointsCount
                info.hasIndexedVectorsCount() && info.indexedVectorsCount > 0L -> info.indexedVectorsCount
                info.hasVectorsCount() && info.vectorsCount > 0L -> info.vectorsCount
                else -> totalChunksCount.toLong()
            }
        } catch (e: Exception) {
            log.warn("Не удалось получить кол-во векторов из Qdrant: {}", e.message)
            totalChunksCount.toLong()
        }
    }

    fun isQdrantConnected(): Boolean {
        return try {
            qdrantClient.listCollectionsAsync().get()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun collectSupportedFiles(rootDir: File): List<File> {
        val rootPath = rootDir.toPath().toAbsolutePath().normalize()
        val rootName = rootDir.name.lowercase()
        return rootDir
            .walkTopDown()
            .onEnter { current ->
                current == rootDir || current.name !in IGNORED_DIRECTORIES
            }
            .filter { file ->
                file.isFile &&
                    file.extension.lowercase() in SUPPORTED_EXTENSIONS &&
                    isRelevantDocumentation(rootPath, rootName, file)
            }
            .sortedBy { it.relativeTo(rootDir).path.lowercase() }
            .toList()
    }

    private fun isRelevantDocumentation(rootPath: Path, rootName: String, file: File): Boolean {
        if (rootName == "docs" || rootName == "rag_files") {
            return true
        }

        val relativePath = rootPath.relativize(file.toPath().toAbsolutePath().normalize())
            .toString()
            .replace('\\', '/')
            .lowercase()
        val fileName = file.name.lowercase()

        if (fileName.startsWith("readme")) {
            return true
        }

        return relativePath.startsWith("docs/") ||
            relativePath.startsWith("project/docs/") ||
            relativePath.startsWith("rag_files/")
    }

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
        return when (file.extension.lowercase()) {
            "md" -> Files.readString(file.toPath())
            "docx" -> extractDocxText(file)
            else -> ""
        }
    }

    private fun extractDocxText(file: File): String {
        val sb = StringBuilder()
        FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { doc ->
                for (paragraph in doc.paragraphs) {
                    val text = paragraph.text.trim()
                    if (text.isNotEmpty()) {
                        sb.appendLine(text)
                    }
                }
                for (table in doc.tables) {
                    for (row in table.rows) {
                        val rowText = row.tableCells.joinToString(" | ") { it.text.trim() }
                        if (rowText.isNotBlank()) {
                            sb.appendLine(rowText)
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun chunkText(text: String): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= CHUNK_SIZE_WORDS) {
            return listOf(words.joinToString(" "))
        }

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
            val request = Points.SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(queryEmbedding.vectorAsList())
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(false))
                .setLimit(topK.toLong())
                .build()
            qdrantClient.searchAsync(request).get().mapNotNull(::toSearchResult)
        } catch (e: Exception) {
            log.error("Ошибка поиска, queryLength={}", query.length, e)
            emptyList()
        }
    }

    fun getIndexedFilesCount(): Int = indexedFilesCount
    fun getTotalChunksCount(): Int = getChunkCount()

    private fun toSearchResult(point: Points.ScoredPoint): SearchResult? {
        val payload = point.payloadMap
        val text = payload["text_segment"]?.stringValue?.takeIf { it.isNotBlank() } ?: return null

        return SearchResult(
            text = text,
            score = point.score.toDouble(),
            sourceFile = payload["source_file"].toPayloadString() ?: "unknown",
            chunkIndex = payload["chunk_index"].toPayloadString()?.toIntOrNull() ?: 0
        )
    }

    private fun JsonWithInt.Value?.toPayloadString(): String? {
        val value = this ?: return null
        return when {
            value.hasStringValue() -> value.stringValue
            value.hasIntegerValue() -> value.integerValue.toString()
            value.hasDoubleValue() -> value.doubleValue.toString()
            value.hasBoolValue() -> value.boolValue.toString()
            else -> null
        }
    }

    data class IndexingCallback(
        var totalFiles: Int = 0,
        var processedFiles: Int = 0,
        var currentFile: String? = null,
        var totalChunks: Int = 0
    )
}
