package com.danichapps.simpleagent.domain.usecase

import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ChatRepository
import kotlinx.serialization.json.Json


private val json = Json { ignoreUnknownKeys = true }

class ExtractTaskStateUseCase {
    suspend operator fun invoke(history: List<Message>, current: TaskState, chatRepository: ChatRepository): TaskState {
        val userAndAssistant = history.filter { it.role == "user" || it.role == "assistant" }
        if (userAndAssistant.isEmpty()) return current

        val conversation = userAndAssistant.joinToString("\n") { msg ->
            val label = if (msg.role == "user") "User" else "Assistant"
            "$label: ${msg.content}"
        }

        val systemMsg = Message(
            role = "system",
            content = """You extract task state from conversations. Return a JSON object with:
- "goal": string - what the user ultimately wants to achieve (empty string if unclear)
- "clarifications": array of strings - things the user has already clarified or specified
- "constraints": array of strings - technical terms, limits, or requirements mentioned
Merge new information with the previous state. Keep existing entries unless contradicted."""
        )

        val userMsg = Message(
            role = "user",
            content = "Conversation:\n$conversation\n\nPrevious state:\n${
                json.encodeToString(TaskState.serializer(), current)
            }"
        )

        return try {
            val response = chatRepository.sendMessages(listOf(systemMsg, userMsg), jsonMode = true)
            json.decodeFromString(TaskState.serializer(), response)
        } catch (e: Exception) {
            current
        }
    }
}
