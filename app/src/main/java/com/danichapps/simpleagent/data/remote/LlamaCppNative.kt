package com.danichapps.simpleagent.data.remote

class LlamaCppNative {
    private val libraryLoaded = runCatching {
        System.loadLibrary("simpleagent_llama")
        true
    }.getOrDefault(false)

    fun isLibraryLoaded(): Boolean = libraryLoaded

    fun isBackendAvailable(): Boolean =
        libraryLoaded && nativeIsBackendAvailable()

    fun isModelReady(): Boolean =
        libraryLoaded && nativeIsModelReady()

    fun initialize(modelPath: String): String? {
        if (!libraryLoaded) return "Native library simpleagent_llama is not loaded"
        return nativeInitialize(modelPath)
    }

    fun generate(prompt: String, maxTokens: Int): String {
        check(libraryLoaded) { "Native library simpleagent_llama is not loaded" }
        return nativeGenerate(prompt, maxTokens)
    }

    fun embed(text: String): FloatArray? {
        if (!libraryLoaded) return null
        return nativeEmbed(text)
    }

    fun release() {
        if (libraryLoaded) nativeRelease()
    }

    private external fun nativeIsBackendAvailable(): Boolean
    private external fun nativeIsModelReady(): Boolean
    private external fun nativeInitialize(modelPath: String): String?
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeEmbed(text: String): FloatArray?
    private external fun nativeRelease()
}
