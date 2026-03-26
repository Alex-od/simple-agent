package com.danichapps.simpleagent.data.local

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalEmbeddingService(
    private val context: Context,
    private val modelPath: String
) {

    private var embedder: TextEmbedder? = null

    fun isModelAvailable(): Boolean = File(modelPath).exists()

    fun initialize() {
        if (!isModelAvailable()) return
        val options = TextEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(modelPath)
                    .build()
            )
            .build()
        embedder = TextEmbedder.createFromOptions(context, options)
    }

    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (!isModelAvailable()) return@withContext null
        if (embedder == null) initialize()
        runCatching {
            val result = embedder?.embed(text) ?: return@withContext null
            val floatList = result.embeddingResult().embeddings()[0].floatEmbedding()
            FloatArray(floatList.size) { i -> floatList[i] }
        }.getOrNull()
    }

    fun release() {
        embedder?.close()
        embedder = null
    }
}
