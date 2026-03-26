package com.danichapps.simpleagent.data.repository

import android.content.Context
import com.danichapps.simpleagent.data.local.BM25Scorer
import com.danichapps.simpleagent.data.local.GemmaRerankService
import com.danichapps.simpleagent.data.local.LocalEmbeddingService
import com.danichapps.simpleagent.data.local.LocalRagChunksDataSource
import com.danichapps.simpleagent.data.local.ScoredChunk
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.repository.RagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.sqrt

private const val EMBEDDINGS_CACHE_FILENAME = "rag_embeddings.bin"

class LocalRagRepositoryImpl(
    private val chunksDataSource: LocalRagChunksDataSource,
    private val embeddingService: LocalEmbeddingService,
    private val context: Context,
    private val rerankService: GemmaRerankService
) : RagRepository {

    private var cachedEmbeddings: List<Pair<RagChunk, FloatArray>>? = null
    private var bm25Scorer: BM25Scorer? = null

    override suspend fun searchContext(query: String, topK: Int): List<RagChunk> {
        if (!embeddingService.isModelAvailable()) return emptyList()

        val embeddings = getOrComputeEmbeddings() ?: return emptyList()
        val queryEmbedding = embeddingService.embed(query) ?: return emptyList()

        val bm25 = bm25Scorer ?: BM25Scorer(embeddings.map { it.first.text }).also { bm25Scorer = it }
        val bm25Scores = embeddings.indices.map { i -> bm25.score(query, i) }
        val maxBm25 = bm25Scores.maxOrNull()?.takeIf { it > 0f } ?: 1f

        val scored = embeddings.mapIndexed { i, (chunk, embedding) ->
            val cosine = cosineSimilarity(queryEmbedding, embedding)
            val bm25Norm = bm25Scores[i] / maxBm25
            ScoredChunk(chunk, cosine * 0.6f + bm25Norm * 0.4f)
        }

        return rerankService.rerank(query, scored)
    }

    private suspend fun getOrComputeEmbeddings(): List<Pair<RagChunk, FloatArray>>? {
        cachedEmbeddings?.let { return it }

        val diskCache = loadEmbeddingsFromDisk()
        if (diskCache != null) {
            cachedEmbeddings = diskCache
            return diskCache
        }

        return computeAndCacheEmbeddings()
    }

    private suspend fun computeAndCacheEmbeddings(): List<Pair<RagChunk, FloatArray>>? =
        withContext(Dispatchers.IO) {
            val chunks = chunksDataSource.getChunks()
            if (chunks.isEmpty()) return@withContext null

            val result = chunks.mapNotNull { chunk ->
                val embedding = embeddingService.embed(chunk.text) ?: return@mapNotNull null
                chunk to embedding
            }

            if (result.isNotEmpty()) saveEmbeddingsToDisk(result)
            cachedEmbeddings = result
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
        }
    }

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
