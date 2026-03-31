package com.danichapps.simpleagent.domain.usecase

import android.util.Log
import com.danichapps.simpleagent.data.remote.ReviewService
import com.danichapps.simpleagent.data.remote.dto.ReviewFindingDto
import com.danichapps.simpleagent.data.remote.dto.ReviewResponseDto
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RoutedChatCommand
import com.danichapps.simpleagent.domain.model.TaskState

private const val HELP_COMMAND_PREFIX = "/help"
private const val REVIEW_COMMAND_PREFIX = "/review"

class CommandRouterUseCase(
    private val buildHelpContextUseCase: BuildHelpContextUseCase,
    private val buildHelpPromptUseCase: BuildHelpPromptUseCase,
    private val reviewService: ReviewService
) {

    suspend fun execute(
        rawInput: String,
        historyWithUser: List<Message>,
        ragEnabled: Boolean,
        taskState: TaskState
    ): RoutedChatCommand {
        return when {
            rawInput.startsWith(HELP_COMMAND_PREFIX) -> {
                val helpQuestion = rawInput.removePrefix(HELP_COMMAND_PREFIX).trim()
                val helpContext = buildHelpContextUseCase.execute(helpQuestion)
                RoutedChatCommand.Prepared(
                    prompt = buildHelpPromptUseCase.execute(helpContext)
                )
            }

            rawInput.startsWith(REVIEW_COMMAND_PREFIX) -> {
                val baseBranch = rawInput.removePrefix(REVIEW_COMMAND_PREFIX).trim()
                    .ifBlank { "master" }
                Log.d("qqwe_tag CommandRouter", "review: baseBranch=$baseBranch")
                val response = reviewService.reviewPr(baseBranch)
                RoutedChatCommand.DirectResponse(content = formatReviewResponse(response))
            }

            else -> RoutedChatCommand.Default(
                messages = historyWithUser,
                ragEnabled = ragEnabled,
                taskState = taskState
            )
        }
    }

    private fun formatReviewResponse(response: ReviewResponseDto): String = buildString {
        appendLine("## AI Code Review")
        appendLine("**Ветка:** ${response.branch ?: "—"} | **Модель:** ${response.model} | **RAG:** ${response.ragContextUsed}")
        appendLine()
        appendLine("### Резюме")
        appendLine(response.summary)

        if (response.bugs.isNotEmpty()) {
            appendLine()
            appendLine("### Потенциальные баги")
            response.bugs.forEach { appendLine(formatFinding(it)) }
        }

        if (response.architecturalIssues.isNotEmpty()) {
            appendLine()
            appendLine("### Архитектурные проблемы")
            response.architecturalIssues.forEach { appendLine(formatFinding(it)) }
        }

        if (response.recommendations.isNotEmpty()) {
            appendLine()
            appendLine("### Рекомендации")
            response.recommendations.forEach { appendLine(formatFinding(it)) }
        }
    }

    private fun formatFinding(finding: ReviewFindingDto): String {
        val icon = when (finding.severity) {
            "critical" -> "[critical]"
            "warning" -> "[warning]"
            else -> "[info]"
        }
        val location = listOfNotNull(finding.file, finding.line)
            .joinToString(":")
            .let { if (it.isNotBlank()) " (*$it*)" else "" }
        return "- **$icon** ${finding.description}$location"
    }
}
