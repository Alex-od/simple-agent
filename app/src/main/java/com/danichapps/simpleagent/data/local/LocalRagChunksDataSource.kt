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
private const val CACHE_SCHEMA_VERSION = 2
private const val CHUNK_SIZE_WORDS = 150
private const val OVERLAP_WORDS = 30

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
                val text = extractText(document) ?: return@forEach
                addAll(chunkText(text, document.name))
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

    private fun extractText(document: RagDocument): String? {
        val uri = android.net.Uri.parse(document.uri)
        return context.contentResolver.openInputStream(uri)?.use { input ->
            when {
                document.name.endsWith(".docx", ignoreCase = true) -> DocxTextExtractor().extract(input)
                document.name.endsWith(".txt", ignoreCase = true) || document.name.endsWith(".md", ignoreCase = true) ->
                    input.bufferedReader().readText()
                else -> null
            }
        }
    }

    private fun chunkText(text: String, source: String): List<RagChunk> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= CHUNK_SIZE_WORDS) {
            return listOf(RagChunk(source = source, chunkIndex = 0, text = words.joinToString(" ")))
        }

        val chunks = mutableListOf<RagChunk>()
        var start = 0
        var index = 0
        while (start < words.size) {
            val end = minOf(start + CHUNK_SIZE_WORDS, words.size)
            chunks.add(RagChunk(source = source, chunkIndex = index++, text = words.subList(start, end).joinToString(" ")))
            start += CHUNK_SIZE_WORDS - OVERLAP_WORDS
        }
        return chunks
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
