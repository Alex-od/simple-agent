package com.danichapps.simpleagent.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlamaCppEmbeddingService(
    private val nativeBackend: LlamaCppNative
) {
    fun isAvailable(): Boolean = nativeBackend.isLibraryLoaded() && nativeBackend.isModelReady()

    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (!isAvailable()) return@withContext null
        nativeBackend.embed(text)
    }
}
