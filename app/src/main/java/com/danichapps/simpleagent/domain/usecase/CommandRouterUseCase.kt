package com.danichapps.simpleagent.domain.usecase

import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RoutedChatCommand
import com.danichapps.simpleagent.domain.model.TaskState

private const val HELP_COMMAND_PREFIX = "/help"

class CommandRouterUseCase(
    private val buildHelpContextUseCase: BuildHelpContextUseCase,
    private val buildHelpPromptUseCase: BuildHelpPromptUseCase
) {

    suspend fun execute(
        rawInput: String,
        historyWithUser: List<Message>,
        ragEnabled: Boolean,
        taskState: TaskState
    ): RoutedChatCommand {
        return if (rawInput.startsWith(HELP_COMMAND_PREFIX)) {
            val helpQuestion = rawInput.removePrefix(HELP_COMMAND_PREFIX).trim()
            val helpContext = buildHelpContextUseCase.execute(helpQuestion)
            RoutedChatCommand.Prepared(
                prompt = buildHelpPromptUseCase.execute(helpContext)
            )
        } else {
            RoutedChatCommand.Default(
                messages = historyWithUser,
                ragEnabled = ragEnabled,
                taskState = taskState
            )
        }
    }
}
