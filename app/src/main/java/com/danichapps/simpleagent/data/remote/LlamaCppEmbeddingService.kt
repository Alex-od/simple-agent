package com.danichapps.simpleagent.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "LlamaCppEmbedding"

class LlamaCppEmbeddingService(
    private val nativeBackend: LlamaCppNative,
    private val modelPathResolver: EmbeddingModelPathResolver
) {
    private var initialized = false

    fun hasConfiguredModel(): Boolean = modelPathResolver.resolveModelFile() != null

    fun isAvailable(): Boolean = nativeBackend.isLibraryLoaded() && nativeBackend.isEmbeddingModelReady()

    fun ensureInitialized(): Boolean {
        if (isAvailable()) {
            initialized = true
            return true
        }
        val modelFile = modelPathResolver.resolveModelFile() ?: return false
        val error = nativeBackend.initializeEmbeddingModel(modelFile.absolutePath)
        if (error != null) {
            Log.e(TAG, "ensureInitialized: $error")
            return false
        }
        initialized = true
        return true
    }

    fun release() {
        nativeBackend.releaseEmbeddingModel()
        initialized = false
    }

    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if ((!initialized && !ensureInitialized()) || !isAvailable()) return@withContext null
        nativeBackend.embed(text)
    }
}
