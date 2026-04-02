package com.danichapps.ragserver.support

import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import com.danichapps.ragserver.service.RagService
import com.danichapps.ragserver.support.dto.SupportRequest
import com.danichapps.ragserver.support.dto.SupportResponse
import com.danichapps.ragserver.support.dto.TicketDto
import com.danichapps.ragserver.support.dto.TicketsFile
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class SupportService(
    private val ragService: RagService,
    private val ollamaClient: OllamaClient,
    private val llmService: LlmService,
    @Value("\${simpleagent.project-root:..}") projectRoot: String
) {

    private val log = LoggerFactory.getLogger(SupportService::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val ticketsPath: File = File(projectRoot).absoluteFile.normalize()
        .resolve("rag_files/support/tickets.json")

    private var ticketsCache: List<TicketDto> = emptyList()

    @PostConstruct
    fun loadTickets() {
        try {
            if (!ticketsPath.exists()) {
                log.warn("qqwe_tag SupportService: tickets.json not found at {}", ticketsPath)
                return
            }
            val file: TicketsFile = objectMapper.readValue(ticketsPath)
            ticketsCache = file.tickets
            log.info("qqwe_tag SupportService: loaded {} tickets from {}", ticketsCache.size, ticketsPath)
        } catch (e: Exception) {
            log.warn("qqwe_tag SupportService: failed to load tickets: {}", e.message)
        }
    }

    fun listTickets(): List<TicketDto> = ticketsCache

    fun handleRequest(request: SupportRequest): SupportResponse {
        val ticket = request.ticketId?.let { id -> ticketsCache.find { it.id == id } }

        val ragQuery = "${request.question} ${ticket?.category ?: ""}".trim()
        val ragContext = try {
            ragService.search(ragQuery, minOf(request.topK, 3))
                .joinToString("\n\n") { it.text }
                .take(600)
        } catch (e: Exception) {
            log.warn("qqwe_tag SupportService: RAG search failed: {}", e.message)
            ""
        }

        val userMessage = buildUserMessage(request.question, ticket, ragContext)
        val model = llmService.getActiveName() ?: DEFAULT_MODEL

        log.info(
            "qqwe_tag SupportService: question='{}', ticketId={}, ragUsed={}, model={}",
            request.question, request.ticketId, ragContext.isNotBlank(), model
        )

        val messages = listOf(
            mapOf("role" to "system", "content" to SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to userMessage)
        )

        val answer = ollamaClient.chat(model, messages)

        return SupportResponse(
            answer = answer,
            ticketId = ticket?.id,
            ticketSubject = ticket?.subject,
            ragContextUsed = ragContext.isNotBlank(),
            model = model
        )
    }

    private fun buildUserMessage(question: String, ticket: TicketDto?, ragContext: String): String =
        buildString {
            if (ticket != null) {
                appendLine("## Данные тикета:")
                appendLine("- ID: ${ticket.id}")
                appendLine("- Пользователь: ${ticket.userName}")
                appendLine("- Категория: ${ticket.category}")
                appendLine("- Тема: ${ticket.subject}")
                appendLine("- Описание: ${ticket.description}")
                appendLine("- Версия приложения: ${ticket.appVersion}")
                appendLine()
            }
            if (ragContext.isNotBlank()) {
                appendLine("## Контекст из документации:")
                appendLine(ragContext)
                appendLine()
            }
            appendLine("## Вопрос пользователя:")
            appendLine(question)
        }

    companion object {
        private const val DEFAULT_MODEL = "llama3.2:3b"

        private val SYSTEM_PROMPT = """
            Ты — AI-ассистент поддержки приложения SimpleAgent.
            Отвечай на русском языке кратко и конкретно.
            Если предоставлены данные тикета — учитывай их контекст в ответе.
            Если есть контекст из документации — используй его для точного ответа.
            Не придумывай функции или возможности, которых нет в документации.
        """.trimIndent()
    }
}
