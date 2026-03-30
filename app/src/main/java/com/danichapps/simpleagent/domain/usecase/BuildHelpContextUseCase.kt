package com.danichapps.simpleagent.domain.usecase

import com.danichapps.simpleagent.domain.model.HelpCommandContext
import com.danichapps.simpleagent.domain.repository.ProjectContextRepository
import com.danichapps.simpleagent.domain.repository.RagRepository

private const val DEFAULT_HELP_QUESTION =
    "Опиши структуру проекта, ключевые модули, API rag-server и как работает команда /help."

class BuildHelpContextUseCase(
    private val ragRepository: RagRepository,
    private val projectContextRepository: ProjectContextRepository
) {

    suspend fun execute(rawQuestion: String): HelpCommandContext {
        val normalizedQuestion = rawQuestion.trim().ifBlank { DEFAULT_HELP_QUESTION }
        val ragQuery = "SimpleAgent project documentation $normalizedQuestion"
        val ragChunks = ragRepository.searchContext(ragQuery)
        val projectContext = runCatching { projectContextRepository.getProjectContext() }.getOrNull()
        return HelpCommandContext(
            question = normalizedQuestion,
            ragChunks = ragChunks,
            projectContext = projectContext
        )
    }
}
