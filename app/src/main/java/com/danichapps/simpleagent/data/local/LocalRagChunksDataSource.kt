package com.danichapps.simpleagent.data.local

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.danichapps.simpleagent.domain.model.RagChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val CACHE_FILENAME = "rag_chunks.json"
private const val CACHE_SCHEMA_VERSION = 4
private const val MAX_CHUNK_CHARS = 360
private const val MAX_HEADER_CHARS = 220
private const val MIN_PARAGRAPH_CHARS = 40

@Serializable
private data class RagChunkDto(val source: String, val chunkIndex: Int, val text: String)

@Serializable
private data class RagDocumentDto(
    val uri: String,
    val name: String,
    val size: Long,
    val lastModified: Long
)

@Serializable
private data class ChunksCache(
    val version: Int,
    val folderUri: String,
    val documents: List<RagDocumentDto>,
    val chunks: List<RagChunkDto>
)

private data class RagDocument(
    val uri: String,
    val name: String,
    val size: Long,
    val lastModified: Long
)

class LocalRagChunksDataSource(
    private val context: Context,
    private val folderPreferences: RagFolderPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File get() = File(context.filesDir, CACHE_FILENAME)

    suspend fun getChunks(): List<RagChunk> = withContext(Dispatchers.IO) {
        val folderUri = folderPreferences.getTreeUri() ?: return@withContext emptyList()
        val documents = listDocuments(folderUri)
        if (documents.isEmpty()) return@withContext emptyList()
        loadFromCache(folderUri.toString(), documents) ?: buildAndCache(folderUri.toString(), documents)
    }

    fun hasCachedIndex(): Boolean {
        val folderUri = folderPreferences.getTreeUriString() ?: return false
        if (!cacheFile.exists()) return false
        return runCatching {
            val cache = json.decodeFromString<ChunksCache>(cacheFile.readText())
            cache.version == CACHE_SCHEMA_VERSION && cache.folderUri == folderUri && cache.chunks.isNotEmpty()
        }.getOrDefault(false)
    }

    private fun loadFromCache(folderUri: String, documents: List<RagDocument>): List<RagChunk>? {
        val file = cacheFile
        if (!file.exists()) return null
        return runCatching {
            val cache = json.decodeFromString<ChunksCache>(file.readText())
            if (cache.version != CACHE_SCHEMA_VERSION) return null
            if (cache.folderUri != folderUri) return null
            if (cache.documents != documents.map { it.toDto() }) return null
            cache.chunks.map { RagChunk(source = it.source, chunkIndex = it.chunkIndex, text = it.text) }
        }.getOrNull()
    }

    private fun buildAndCache(folderUri: String, documents: List<RagDocument>): List<RagChunk> {
        val chunks = buildList {
            documents.forEach { document ->
                val paragraphs = extractParagraphs(document)
                if (paragraphs.isEmpty()) return@forEach
                addAll(chunkParagraphs(paragraphs, document.name))
            }
        }
        saveToCache(folderUri, documents, chunks)
        return chunks
    }

    private fun listDocuments(folderUri: android.net.Uri): List<RagDocument> {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        return folder.listFiles()
            .filter { it.isFile && it.name?.let(::isSupportedDocument) == true }
            .sortedBy { it.name.orEmpty().lowercase() }
            .map {
                RagDocument(
                    uri = it.uri.toString(),
                    name = it.name.orEmpty(),
                    size = it.length(),
                    lastModified = it.lastModified()
                )
            }
    }

    private fun extractParagraphs(document: RagDocument): List<String> {
        val uri = android.net.Uri.parse(document.uri)
        return context.contentResolver.openInputStream(uri)?.use { input ->
            when {
                document.name.endsWith(".docx", ignoreCase = true) -> DocxTextExtractor()
                    .extract(input)
                    .lineSequence()
                    .map(String::trim)
                    .filter { it.length >= MIN_PARAGRAPH_CHARS }
                    .toList()
                document.name.endsWith(".txt", ignoreCase = true) || document.name.endsWith(".md", ignoreCase = true) ->
                    input.bufferedReader()
                        .readText()
                        .split(Regex("\\r?\\n\\s*\\r?\\n|\\r?\\n"))
                        .map(String::trim)
                        .filter { it.length >= MIN_PARAGRAPH_CHARS }
                else -> emptyList()
            }
        } ?: emptyList()
    }

    private fun chunkParagraphs(paragraphs: List<String>, source: String): List<RagChunk> {
        if (paragraphs.isEmpty()) return emptyList()

        val chunks = mutableListOf<RagChunk>()
        var index = 0
        var start = 0

        val headerChunk = buildString {
            for (paragraph in paragraphs) {
                if (isNotBlank()) append("\n")
                append(paragraph)
                if (length >= MAX_HEADER_CHARS) break
            }
        }.trim()

        if (headerChunk.isNotBlank()) {
            chunks.add(RagChunk(source = source, chunkIndex = index++, text = headerChunk))
            start = headerParagraphCount(paragraphs)
        }

        while (start in paragraphs.indices) {
            val chunkText = buildString {
                var cursor = start
                while (cursor < paragraphs.size) {
                    val addition = if (isBlank()) paragraphs[cursor] else "\n\n${paragraphs[cursor]}"
                    if ((length + addition.length) > MAX_CHUNK_CHARS && cursor > start) break
                    append(addition)
                    cursor++
                    if (length >= MAX_CHUNK_CHARS) break
                }
            }.trim()

            if (chunkText.isNotBlank()) {
                chunks.add(RagChunk(source = source, chunkIndex = index++, text = chunkText.take(MAX_CHUNK_CHARS)))
            }

            val nextStart = nextChunkStart(paragraphs, start)
            if (nextStart <= start) break
            start = nextStart
        }

        return chunks
    }

    private fun nextChunkStart(paragraphs: List<String>, start: Int): Int {
        var consumedChars = 0
        var cursor = start
        while (cursor < paragraphs.size) {
            consumedChars += paragraphs[cursor].length + 2
            cursor++
            if (consumedChars >= MAX_CHUNK_CHARS) break
        }
        if (cursor >= paragraphs.size) return paragraphs.size
        return cursor
    }

    private fun headerParagraphCount(paragraphs: List<String>): Int {
        var consumedChars = 0
        var cursor = 0
        while (cursor < paragraphs.size) {
            consumedChars += paragraphs[cursor].length + 1
            cursor++
            if (consumedChars >= MAX_HEADER_CHARS) break
        }
        return cursor.coerceAtLeast(1)
    }

    private fun saveToCache(folderUri: String, documents: List<RagDocument>, chunks: List<RagChunk>) {
        runCatching {
            val dtos = chunks.map { RagChunkDto(source = it.source, chunkIndex = it.chunkIndex, text = it.text) }
            val cache = ChunksCache(
                version = CACHE_SCHEMA_VERSION,
                folderUri = folderUri,
                documents = documents.map { it.toDto() },
                chunks = dtos
            )
            cacheFile.writeText(json.encodeToString(cache))
        }
    }

    private fun RagDocument.toDto(): RagDocumentDto = RagDocumentDto(
        uri = uri,
        name = name,
        size = size,
        lastModified = lastModified
    )

    private fun isSupportedDocument(name: String): Boolean =
        name.endsWith(".docx", ignoreCase = true) ||
            name.endsWith(".txt", ignoreCase = true) ||
            name.endsWith(".md", ignoreCase = true)
}
