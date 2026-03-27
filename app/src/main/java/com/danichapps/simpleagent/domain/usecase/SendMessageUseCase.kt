package com.danichapps.simpleagent.domain.usecase

import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.model.RagSource
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private fun RagChunk.toSource(): RagSource = RagSource(
    source = source,
    chunkIndex = chunkIndex,
    quote = if (text.length > 150) text.take(150) + "..." else text
)

class SendMessageUseCase(
    private val ragRepository: RagRepository
) {

    suspend operator fun invoke(
        messages: List<Message>,
        chatRepository: ChatRepository,
        ragEnabled: Boolean = false,
        taskState: TaskState = TaskState(),
        settings: ChatTuningSettings = ChatTuningSettings()
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
            if (settings.systemPrompt.isNotBlank()) {
                appendLine(settings.systemPrompt.trim())
            }
            appendLine("Ты полезный ассистент. Всегда отвечай ТОЛЬКО на русском языке, даже если вопрос задан на другом языке.")
            if (!taskState.isEmpty()) {
                appendLine()
                appendLine("---ПАМЯТЬ ЗАДАЧИ---")
                if (taskState.goal.isNotBlank()) appendLine("Цель: ${taskState.goal}")
                if (taskState.clarifications.isNotEmpty())
                    appendLine("Уточнения: ${taskState.clarifications.joinToString("; ")}")
                if (taskState.constraints.isNotEmpty())
                    appendLine("Ограничения/Термины: ${taskState.constraints.joinToString("; ")}")
                appendLine("---КОНЕЦ ПАМЯТИ---")
            }
            if (chunks.isNotEmpty()) {
                val context = chunks.joinToString("\n\n---\n\n") { it.text }
                append("Отвечай ТОЛЬКО на основе контекста ниже. ")
                append("Если контекст не содержит точного ответа — напиши: 'В документе информация не найдена.' ")
                append("Не придумывай и не дополняй из своих знаний.\n\n")
                append("---КОНТЕКСТ---\n$context\n---КОНЕЦ КОНТЕКСТА---")
            }
        }

        val enrichedMessages = if (systemContent.isNotBlank()) {
            listOf(Message(role = "system", content = systemContent.trim())) + messages
        } else {
            messages
        }

        val answer = chatRepository.sendMessages(enrichedMessages, settings = settings)
        val sources = chunks.map { it.toSource() }
        return Pair(answer, sources)
    }

    fun invokeStreaming(
        messages: List<Message>,
        chatRepository: ChatRepository,
        ragEnabled: Boolean = false,
        taskState: TaskState = TaskState(),
        settings: ChatTuningSettings = ChatTuningSettings()
    ): Flow<String> {
        // RAG требует suspend для поиска контекста — используем suspend invoke() и эмитим одним событием
        if (ragEnabled) {
            return flow {
                val (answer, _) = invoke(messages, chatRepository, ragEnabled, taskState, settings)
                emit(answer)
            }
        }

        val systemContent = buildString {
            if (settings.systemPrompt.isNotBlank()) {
                appendLine(settings.systemPrompt.trim())
            }
            appendLine("Ты полезный ассистент. Всегда отвечай ТОЛЬКО на русском языке, даже если вопрос задан на другом языке.")
            if (!taskState.isEmpty()) {
                appendLine()
                appendLine("---ПАМЯТЬ ЗАДАЧИ---")
                if (taskState.goal.isNotBlank()) appendLine("Цель: ${taskState.goal}")
                if (taskState.clarifications.isNotEmpty())
                    appendLine("Уточнения: ${taskState.clarifications.joinToString("; ")}")
                if (taskState.constraints.isNotEmpty())
                    appendLine("Ограничения/Термины: ${taskState.constraints.joinToString("; ")}")
                appendLine("---КОНЕЦ ПАМЯТИ---")
            }
        }

        val enrichedMessages = if (systemContent.isNotBlank()) {
            listOf(Message(role = "system", content = systemContent.trim())) + messages
        } else {
            messages
        }

        return chatRepository.sendMessagesStreaming(enrichedMessages, settings = settings)
    }
}
