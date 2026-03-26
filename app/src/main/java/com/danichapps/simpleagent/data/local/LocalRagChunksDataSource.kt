package com.danichapps.simpleagent.data.local

import android.content.Context
import com.danichapps.simpleagent.domain.model.RagChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val CACHE_FILENAME = "rag_chunks.json"
private const val DOCX_ASSET = "IEEE-830-1998_RU.docx"
private const val CHUNK_SIZE_WORDS = 150
private const val OVERLAP_WORDS = 30

@Serializable
private data class RagChunkDto(val source: String, val chunkIndex: Int, val text: String)

@Serializable
private data class ChunksCache(val chunks: List<RagChunkDto>)

class LocalRagChunksDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File get() = File(context.filesDir, CACHE_FILENAME)

    suspend fun getChunks(): List<RagChunk> = withContext(Dispatchers.IO) {
        loadFromCache() ?: buildAndCache()
    }

    private fun loadFromCache(): List<RagChunk>? {
        val file = cacheFile
        if (!file.exists()) return null
        return runCatching {
            val cache = json.decodeFromString<ChunksCache>(file.readText())
            cache.chunks.map { RagChunk(source = it.source, chunkIndex = it.chunkIndex, text = it.text) }
        }.getOrNull()
    }

    private fun buildAndCache(): List<RagChunk> {
        val text = context.assets.open(DOCX_ASSET).use { DocxTextExtractor().extract(it) }
        val chunks = chunkText(text, DOCX_ASSET)
        saveToCache(chunks)
        return chunks
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

    private fun saveToCache(chunks: List<RagChunk>) {
        runCatching {
            val dtos = chunks.map { RagChunkDto(source = it.source, chunkIndex = it.chunkIndex, text = it.text) }
            cacheFile.writeText(json.encodeToString(ChunksCache(dtos)))
        }
    }
}
