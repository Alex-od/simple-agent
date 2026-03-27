package com.danichapps.simpleagent.domain.usecase

import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.model.RagSource
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.RagRepository

private const val MAX_RAG_CHUNKS_IN_OFFLINE_PROMPT = 1
private const val MAX_RAG_CHARS_PER_OFFLINE_CHUNK = 450

private fun RagChunk.toOfflineSource(): RagSource = RagSource(
    source = source,
    chunkIndex = chunkIndex,
    quote = if (text.length > 150) text.take(150) + "..." else text
)

class OfflineSendMessageUseCase(
    private val ragRepository: RagRepository
) {
    suspend operator fun invoke(
        messages: List<Message>,
        chatRepository: ChatRepository,
        ragEnabled: Boolean = false,
        taskState: TaskState = TaskState()
    ): Pair<String, List<RagSource>> {
        val chunks = if (ragEnabled && messages.isNotEmpty()) {
            val lastUserQuery = messages.last { it.role == "user" }.content
            val ragQuery = if (!taskState.isEmpty()) {
                val extra = (listOfNotNull(taskState.goal.takeIf { it.isNotBlank() }) + taskState.constraints)
                    .joinToString(" ")
                "$lastUserQuery $extra"
            } else {
                lastUserQuery
            }
            ragRepository.searchContext(ragQuery)
        } else {
            emptyList()
        }

        val systemContent = buildString {
            appendLine("Отвечай только на русском языке.")
            if (chunks.isNotEmpty()) {
                val context = chunks
                    .take(MAX_RAG_CHUNKS_IN_OFFLINE_PROMPT)
                    .joinToString("\n\n---\n\n") { chunk ->
                        if (chunk.text.length > MAX_RAG_CHARS_PER_OFFLINE_CHUNK) {
                            chunk.text.take(MAX_RAG_CHARS_PER_OFFLINE_CHUNK) + "..."
                        } else {
                            chunk.text
                        }
                    }
                appendLine("Используй только контекст ниже.")
                appendLine("Если ответа нет, напиши: В документе информация не найдена.")
                appendLine("Не добавляй сведения от себя.")
                appendLine()
                append("---КОНТЕКСТ---\n$context\n---КОНЕЦ КОНТЕКСТА---")
            } else if (!taskState.isEmpty()) {
                appendLine()
                appendLine("---ПАМЯТЬ ЗАДАЧИ---")
                if (taskState.goal.isNotBlank()) appendLine("Цель: ${taskState.goal}")
                if (taskState.clarifications.isNotEmpty()) {
                    appendLine("Уточнения: ${taskState.clarifications.joinToString("; ")}")
                }
                if (taskState.constraints.isNotEmpty()) {
                    appendLine("Ограничения: ${taskState.constraints.joinToString("; ")}")
                }
                appendLine("---КОНЕЦ ПАМЯТИ---")
            }
        }

        val enrichedMessages = if (systemContent.isNotBlank()) {
            listOf(Message(role = "system", content = systemContent.trim())) + messages
        } else {
            messages
        }

        val answer = chatRepository.sendMessages(enrichedMessages)
        val sources = chunks.map { it.toOfflineSource() }
        return Pair(answer, sources)
    }
}
