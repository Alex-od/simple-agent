package com.danichapps.simpleagent.domain.usecase

import android.util.Log
import com.danichapps.simpleagent.data.remote.ReviewService
import com.danichapps.simpleagent.data.remote.SupportService
import com.danichapps.simpleagent.data.remote.dto.ReviewResponseDto
import com.danichapps.simpleagent.data.remote.dto.SupportResponseDto
import com.danichapps.simpleagent.data.remote.dto.TicketSummaryDto
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RoutedChatCommand
import com.danichapps.simpleagent.domain.model.TaskState

private const val HELP_COMMAND_PREFIX = "/help"
private const val REVIEW_COMMAND_PREFIX = "/review"
private const val SUPPORT_COMMAND_PREFIX = "/support"

class CommandRouterUseCase(
    private val buildHelpContextUseCase: BuildHelpContextUseCase,
    private val buildHelpPromptUseCase: BuildHelpPromptUseCase,
    private val reviewService: ReviewService,
    private val supportService: SupportService
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

            rawInput.startsWith(SUPPORT_COMMAND_PREFIX) -> {
                val rest = rawInput.removePrefix(SUPPORT_COMMAND_PREFIX).trim()
                Log.d("qqwe_tag CommandRouter", "support: rest='$rest'")
                if (rest.isBlank()) {
                    val tickets = supportService.listTickets()
                    RoutedChatCommand.DirectResponse(content = formatTicketList(tickets))
                } else {
                    val ticketId = if (rest.startsWith("#")) {
                        rest.split(" ").first().removePrefix("#").ifBlank { null }
                    } else null
                    val question = if (ticketId != null) {
                        rest.removePrefix("#$ticketId").trim()
                    } else rest
                    val response = supportService.ask(question, ticketId)
                    RoutedChatCommand.DirectResponse(content = formatSupportResponse(response))
                }
            }

            else -> RoutedChatCommand.Default(
                messages = historyWithUser,
                ragEnabled = ragEnabled,
                taskState = taskState
            )
        }
    }

    private fun formatSupportResponse(r: SupportResponseDto): String = buildString {
        appendLine("## Поддержка")
        if (r.ticketId != null) appendLine("**Тикет:** ${r.ticketId} — ${r.ticketSubject}")
        appendLine("**Модель:** ${r.model} | **RAG:** ${r.ragContextUsed}")
        appendLine()
        appendLine(r.answer)
    }

    private fun formatTicketList(tickets: List<TicketSummaryDto>): String = buildString {
        appendLine("## Открытые тикеты")
        if (tickets.isEmpty()) {
            appendLine("Нет тикетов.")
            return@buildString
        }
        tickets.forEach { t ->
            appendLine("- **${t.id}** [${t.category}] ${t.subject} — *${t.status}* (${t.userName})")
        }
        appendLine()
        appendLine("Используй `/support #T-001 вопрос` для ответа с учётом тикета.")
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
            response.bugs.forEach { appendLine("- $it") }
        }

        if (response.architecturalIssues.isNotEmpty()) {
            appendLine()
            appendLine("### Архитектурные проблемы")
            response.architecturalIssues.forEach { appendLine("- $it") }
        }

        if (response.recommendations.isNotEmpty()) {
            appendLine()
            appendLine("### Рекомендации")
            response.recommendations.forEach { appendLine("- $it") }
        }
    }
}
