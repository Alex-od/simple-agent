package com.danichapps.simpleagent.data.remote

class LlamaCppNative {
    private val libraryLoaded = runCatching {
        System.loadLibrary("simpleagent_llama")
        true
    }.getOrDefault(false)

    fun isLibraryLoaded(): Boolean = libraryLoaded

    fun isBackendAvailable(): Boolean =
        libraryLoaded && nativeIsBackendAvailable()

    fun isChatModelReady(): Boolean =
        libraryLoaded && nativeIsChatModelReady()

    fun isEmbeddingModelReady(): Boolean =
        libraryLoaded && nativeIsEmbeddingModelReady()

    fun initializeChatModel(modelPath: String): String? {
        if (!libraryLoaded) return "Native library simpleagent_llama is not loaded"
        return nativeInitializeChatModel(modelPath)
    }

    fun initializeEmbeddingModel(modelPath: String): String? {
        if (!libraryLoaded) return "Native library simpleagent_llama is not loaded"
        return nativeInitializeEmbeddingModel(modelPath)
    }

    fun generate(prompt: String, maxTokens: Int, temperature: Float): String {
        check(libraryLoaded) { "Native library simpleagent_llama is not loaded" }
        return nativeGenerate(prompt, maxTokens, temperature)
    }

    fun embed(text: String): FloatArray? {
        if (!libraryLoaded) return null
        return nativeEmbed(text)
    }

    fun releaseChatModel() {
        if (libraryLoaded) nativeReleaseChatModel()
    }

    fun releaseEmbeddingModel() {
        if (libraryLoaded) nativeReleaseEmbeddingModel()
    }

    fun releaseAll() {
        if (libraryLoaded) nativeReleaseAll()
    }

    private external fun nativeIsBackendAvailable(): Boolean
    private external fun nativeIsChatModelReady(): Boolean
    private external fun nativeIsEmbeddingModelReady(): Boolean
    private external fun nativeInitializeChatModel(modelPath: String): String?
    private external fun nativeInitializeEmbeddingModel(modelPath: String): String?
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float): String
    private external fun nativeEmbed(text: String): FloatArray?
    private external fun nativeReleaseChatModel()
    private external fun nativeReleaseEmbeddingModel()
    private external fun nativeReleaseAll()
}
