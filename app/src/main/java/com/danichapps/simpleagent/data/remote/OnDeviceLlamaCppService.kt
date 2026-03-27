package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

private const val MAX_TOKENS = 96
private const val GENERATION_TIMEOUT_MS = 45_000L

class OnDeviceLlamaCppService(
    private val modelPathResolver: DeviceModelPathResolver,
    private val nativeBackend: LlamaCppNative
) : ChatService {

    private var initialized = false

    fun initialize() {
        val modelFile = modelPathResolver.resolveModelFile()
        require(modelFile.exists()) {
            "GGUF model not found on device: ${modelFile.absolutePath}"
        }
        require(nativeBackend.isLibraryLoaded()) {
            "Native library simpleagent_llama is missing"
        }
        val error = nativeBackend.initialize(modelFile.absolutePath)
        check(error == null) { error ?: "Unknown llama.cpp initialization error" }
        initialized = true
    }

    fun release() {
        nativeBackend.release()
        initialized = false
    }

    override suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean): String {
        check(initialized) { "On-device llama.cpp is not initialized" }
        return withContext(Dispatchers.Default) {
            withTimeout(GENERATION_TIMEOUT_MS) {
                nativeBackend.generate(formatPrompt(messages, jsonMode), MAX_TOKENS)
            }
        }
    }

    override fun sendMessagesStreaming(messages: List<MessageDto>, jsonMode: Boolean): Flow<String> = flow {
        emit(sendMessages(messages, jsonMode))
    }

    private fun formatPrompt(messages: List<MessageDto>, jsonMode: Boolean): String {
        val systemMessages = messages.filter { it.role == "system" }.joinToString("\n") { it.content.trim() }
        val dialogMessages = messages.filter { it.role != "system" }

        return buildString {
            append("<|im_start|>system\n")
            if (systemMessages.isNotBlank()) append(systemMessages).append("\n")
            if (jsonMode) append("Respond only with valid JSON.\n")
            append("<|im_end|>\n")

            dialogMessages.forEach { message ->
                val role = if (message.role == "assistant") "assistant" else "user"
                append("<|im_start|>").append(role).append("\n")
                append(message.content.trim()).append("\n")
                append("<|im_end|>\n")
            }

            append("<|im_start|>assistant\n")
        }
    }
}
