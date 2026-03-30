package com.danichapps.simpleagent

import com.danichapps.simpleagent.domain.model.HelpCommandContext
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.ProjectContext
import com.danichapps.simpleagent.domain.model.RagChunk
import com.danichapps.simpleagent.domain.model.RoutedChatCommand
import com.danichapps.simpleagent.domain.model.TaskState
import com.danichapps.simpleagent.domain.repository.ProjectContextRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import com.danichapps.simpleagent.domain.usecase.BuildHelpContextUseCase
import com.danichapps.simpleagent.domain.usecase.BuildHelpPromptUseCase
import com.danichapps.simpleagent.domain.usecase.CommandRouterUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterUseCaseTest {

    @Test
    fun routesHelpCommandToPreparedPrompt() = runBlocking {
        val router = createRouter()

        val result = router.execute(
            rawInput = "/help где находится логика RAG",
            historyWithUser = listOf(Message(role = "user", content = "/help где находится логика RAG")),
            ragEnabled = false,
            taskState = TaskState()
        )

        assertTrue(result is RoutedChatCommand.Prepared)
        val prepared = result as RoutedChatCommand.Prepared
        assertEquals("system", prepared.prompt.messages.first().role)
        assertEquals("где находится логика RAG", prepared.prompt.messages.last().content)
        assertTrue(prepared.prompt.messages.first().content.contains("Current git branch: rag-and-gitmcp"))
        assertTrue(prepared.prompt.sources.isNotEmpty())
    }

    @Test
    fun passesRegularMessageThroughDefaultRoute() = runBlocking {
        val router = createRouter()
        val history = listOf(Message(role = "user", content = "обычный вопрос"))

        val result = router.execute(
            rawInput = "обычный вопрос",
            historyWithUser = history,
            ragEnabled = true,
            taskState = TaskState(goal = "goal")
        )

        assertTrue(result is RoutedChatCommand.Default)
        val defaultRoute = result as RoutedChatCommand.Default
        assertEquals(history, defaultRoute.messages)
        assertTrue(defaultRoute.ragEnabled)
        assertEquals("goal", defaultRoute.taskState.goal)
    }

    private fun createRouter(): CommandRouterUseCase {
        return CommandRouterUseCase(
            buildHelpContextUseCase = BuildHelpContextUseCase(
                ragRepository = FakeRagRepository(),
                projectContextRepository = FakeProjectContextRepository()
            ),
            buildHelpPromptUseCase = BuildHelpPromptUseCase()
        )
    }

    private class FakeRagRepository : RagRepository {
        override suspend fun searchContext(query: String, topK: Int): List<RagChunk> {
            return listOf(
                RagChunk(
                    source = "README.md",
                    chunkIndex = 0,
                    text = "SimpleAgent consists of app and rag-server modules."
                )
            )
        }
    }

    private class FakeProjectContextRepository : ProjectContextRepository {
        override suspend fun getProjectContext(): ProjectContext {
            return ProjectContext(
                branch = "rag-and-gitmcp",
                changedFiles = listOf(
                    "app/src/main/java/com/danichapps/simpleagent/presentation/ChatViewModel.kt"
                ),
                gitRepository = true,
                projectRoot = "C:/StudioProjects/SimpleAgent"
            )
        }
    }
}
