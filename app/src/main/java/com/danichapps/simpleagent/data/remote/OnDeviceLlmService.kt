package com.danichapps.simpleagent.data.remote

import android.content.Context
import com.danichapps.simpleagent.data.remote.dto.MessageDto
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList

private const val MAX_TOKENS = 4096

class OnDeviceLlmService(
    private val context: Context,
    private val modelPath: String
) : ChatService {

    private var llmInference: LlmInference? = null

    fun isInitialized(): Boolean = llmInference != null

    fun initialize() {
        llmInference = try {
            createInference(useGpu = true)
        } catch (e: Exception) {
            createInference(useGpu = false)
        }
    }

    private fun createInference(useGpu: Boolean): LlmInference {
        val backend = if (useGpu) LlmInference.Backend.GPU else LlmInference.Backend.CPU
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(MAX_TOKENS)
            .setPreferredBackend(backend)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    private fun formatPrompt(messages: List<MessageDto>, jsonMode: Boolean): String {
        val sb = StringBuilder()
        // Извлекаем системное сообщение (всегда первое, если есть)
        val systemMsg = messages.firstOrNull { it.role == "system" }?.content
        val dialogMessages = messages.filter { it.role != "system" }

        // Gemma 3 IT chat template
        dialogMessages.forEachIndexed { index, msg ->
            when (msg.role) {
                "user" -> {
                    sb.append("<start_of_turn>user\n")
                    // Системный промпт вставляем перед первым сообщением пользователя
                    if (index == 0 && systemMsg != null) {
                        sb.append(systemMsg).append("\n\n")
                    }
                    if (jsonMode) {
                        sb.append("Respond ONLY with valid JSON. No extra text.\n\n")
                    }
                    sb.append(msg.content).append("<end_of_turn>\n")
                }
                "assistant" -> {
                    sb.append("<start_of_turn>model\n")
                    sb.append(msg.content).append("<end_of_turn>\n")
                }
            }
        }
        // Открываем ход модели
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    override suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean): String {
        val sb = StringBuilder()
        withContext(Dispatchers.IO) {
            sendMessagesStreaming(messages, jsonMode).toList()
        }.forEach { sb.append(it) }
        return sb.toString()
    }

    override fun sendMessagesStreaming(messages: List<MessageDto>, jsonMode: Boolean): Flow<String> {
        val inference = llmInference ?: return flow { error("LlmInference not initialized") }
        val prompt = formatPrompt(messages, jsonMode)
        return callbackFlow {
            val session = LlmInferenceSession.createFromOptions(
                inference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder().build()
            )
            val accumulated = StringBuilder()
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                if (isClosedForSend) return@generateResponseAsync
                if (partial != null) {
                    accumulated.append(partial)
                    if ("<end_of_turn>" in accumulated) {
                        // EOS достигнут — не отправляем этот токен, закрываем flow
                        // session.close() намеренно НЕ вызываем здесь — только в awaitClose
                        close()
                        return@generateResponseAsync
                    } else {
                        trySend(partial)
                    }
                }
                if (done) {
                    close()
                }
            }
            awaitClose { runCatching { session.close() } }
        }
    }

    fun release() {
        llmInference?.close()
        llmInference = null
    }
}
