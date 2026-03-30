package com.danichapps.simpleagent.domain.model

sealed interface RoutedChatCommand {
    data class Default(
        val messages: List<Message>,
        val ragEnabled: Boolean,
        val taskState: TaskState
    ) : RoutedChatCommand

    data class Prepared(
        val prompt: PreparedPrompt
    ) : RoutedChatCommand
}
