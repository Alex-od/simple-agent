package com.danichapps.simpleagent.data.repository

import android.content.Context
import android.util.Log
import com.danichapps.simpleagent.data.local.GemmaRerankService
import com.danichapps.simpleagent.data.local.LocalRagChunksDataSource
import com.danichapps.simpleagent.data.local.ScoredChunk
import com.danichapps.simpleagent.data.remote.LlamaCppEmbeddingService
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.repository.RagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.sqrt

private const val EMBEDDINGS_CACHE_FILENAME = "rag_embeddings_llama.bin"
private const val TAG = "LocalRagRepository"

class LocalRagRepositoryImpl(
    private val chunksDataSource: LocalRagChunksDataSource,
    private val embeddingService: LlamaCppEmbeddingService,
    private val context: Context,
    private val rerankService: GemmaRerankService
) : RagRepository {

    private var cachedEmbeddings: List<Pair<RagChunk, FloatArray>>? = null

    override fun isIndexed(): Boolean = hasEmbeddingsCache()

    override fun hasDocumentCache(): Boolean = chunksDataSource.hasCachedIndex()

    override suspend fun buildIndexIfNeeded() {
        val chunks = chunksDataSource.getChunks()
        if (chunks.isEmpty()) {
            Log.w(TAG, "buildIndexIfNeeded: no chunks found")
            return
        }
        if (!embeddingService.ensureInitialized()) {
            Log.w(TAG, "buildIndexIfNeeded: embedding service is not available")
            return
        }
        val cached = loadEmbeddingsFromDisk()
        if (cached != null && cached.size == chunks.size && cached.map { it.first } == chunks) {
            cachedEmbeddings = cached
            Log.i(TAG, "buildIndexIfNeeded: using cached embeddings count=${cached.size}")
            return
        }
        Log.i(TAG, "buildIndexIfNeeded: computing embeddings for chunks=${chunks.size}")
        computeAndCacheEmbeddings(chunks)
    }

    override suspend fun searchContext(query: String, topK: Int): List<RagChunk> {
        val chunks = chunksDataSource.getChunks()
        if (chunks.isEmpty()) return emptyList()

        val embeddings = getOrComputeEmbeddings()
            ?.takeIf { it.size == chunks.size }
            ?: return emptyList()
        val queryEmbedding = embeddingService.embed(query) ?: return emptyList()

        val scored = chunks.mapIndexed { i, chunk ->
            val cosine = cosineSimilarity(queryEmbedding, embeddings[i].second)
            ScoredChunk(chunk, cosine)
        }

        val reranked = rerankService.rerank(query, scored, topK)
        reranked.firstOrNull()?.let { topChunk ->
            val preview = topChunk.text.replace("\n", " ").take(180)
            Log.i(
                TAG,
                "searchContext: query=\"$query\" topChunk=${topChunk.chunkIndex} source=${topChunk.source} preview=\"$preview\""
            )
        } ?: Log.w(TAG, "searchContext: query=\"$query\" produced no chunks")
        return reranked
    }

    private suspend fun getOrComputeEmbeddings(): List<Pair<RagChunk, FloatArray>>? {
        cachedEmbeddings?.let { return it }

        val diskCache = loadEmbeddingsFromDisk()
        if (diskCache != null) {
            cachedEmbeddings = diskCache
            Log.i(TAG, "getOrComputeEmbeddings: loaded embeddings from disk count=${diskCache.size}")
            return diskCache
        }

        val chunks = chunksDataSource.getChunks()
        if (chunks.isEmpty()) return null
        return computeAndCacheEmbeddings(chunks)
    }

    private suspend fun computeAndCacheEmbeddings(chunks: List<RagChunk>): List<Pair<RagChunk, FloatArray>>? =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Pair<RagChunk, FloatArray>>()
            chunks.forEachIndexed { index, chunk ->
                val embedding = embeddingService.embed(chunk.text)
                if (embedding == null) {
                    Log.e(TAG, "computeAndCacheEmbeddings: embedding failed for chunkIndex=${chunk.chunkIndex} source=${chunk.source} position=$index/${chunks.size}")
                    return@withContext null
                }
                result += chunk to embedding
                Log.d(TAG, "computeAndCacheEmbeddings: embedded chunkIndex=${chunk.chunkIndex} position=${index + 1}/${chunks.size} dim=${embedding.size}")
            }

            saveEmbeddingsToDisk(result)
            cachedEmbeddings = result
            Log.i(TAG, "computeAndCacheEmbeddings: embeddings saved count=${result.size}")
            result
        }

    private fun loadEmbeddingsFromDisk(): List<Pair<RagChunk, FloatArray>>? {
        val file = File(context.filesDir, EMBEDDINGS_CACHE_FILENAME)
        if (!file.exists()) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { stream ->
                val count = stream.readInt()
                List(count) {
                    val source = stream.readUTF()
                    val chunkIndex = stream.readInt()
                    val textLength = stream.readInt()
                    val textBytes = ByteArray(textLength)
                    stream.readFully(textBytes)
                    val text = String(textBytes, Charsets.UTF_8)
                    val dim = stream.readInt()
                    val embedding = FloatArray(dim) { stream.readFloat() }
                    RagChunk(source = source, chunkIndex = chunkIndex, text = text) to embedding
                }
            }
        }.onFailure {
            Log.e(TAG, "loadEmbeddingsFromDisk: failed to read cache", it)
        }.getOrNull()
    }

    private fun saveEmbeddingsToDisk(entries: List<Pair<RagChunk, FloatArray>>) {
        runCatching {
            val file = File(context.filesDir, EMBEDDINGS_CACHE_FILENAME)
            DataOutputStream(file.outputStream().buffered()).use { stream ->
                stream.writeInt(entries.size)
                entries.forEach { (chunk, embedding) ->
                    stream.writeUTF(chunk.source)
                    stream.writeInt(chunk.chunkIndex)
                    val textBytes = chunk.text.toByteArray(Charsets.UTF_8)
                    stream.writeInt(textBytes.size)
                    stream.write(textBytes)
                    stream.writeInt(embedding.size)
                    embedding.forEach { stream.writeFloat(it) }
                }
            }
        }.onFailure {
            Log.e(TAG, "saveEmbeddingsToDisk: failed to write cache", it)
        }
    }

    private fun hasEmbeddingsCache(): Boolean = File(context.filesDir, EMBEDDINGS_CACHE_FILENAME).exists()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f
        else dot / (sqrt(normA) * sqrt(normB))
    }
}
