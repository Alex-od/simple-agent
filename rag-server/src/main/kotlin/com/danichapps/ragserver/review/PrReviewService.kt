package com.danichapps.ragserver.review

import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import com.danichapps.ragserver.project.ProjectGitService
import com.danichapps.ragserver.review.dto.PrReviewRequest
import com.danichapps.ragserver.review.dto.PrReviewResponse
import com.danichapps.ragserver.review.dto.ReviewFinding
import com.danichapps.ragserver.service.RagService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrReviewService(
    private val ragService: RagService,
    private val ollamaClient: OllamaClient,
    private val llmService: LlmService,
    private val projectGitService: ProjectGitService
) {

    private val log = LoggerFactory.getLogger(PrReviewService::class.java)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val MAX_DIFF_CHARS = 24_000
        private const val DEFAULT_MODEL = "llama3.2:3b"
        private val CODE_EXTENSIONS = setOf("kt", "java", "yml", "gradle", "kts", "xml")

        private val SYSTEM_PROMPT = """
            Ты — senior Kotlin/Android код-ревьюер. Анализируй PR diff и давай структурированную обратную связь.
            Отвечай ТОЛЬКО валидным JSON без markdown-обёрток:
            {"summary":"1-2 предложения","bugs":[...],"architecturalIssues":[...],"recommendations":[...]}
            Каждый finding: {"severity":"critical|warning|info","file":"path или null","line":"ref или null","description":"описание"}
            Фокус: null safety, утечки ресурсов, concurrency, lifecycle, архитектурные нарушения, безопасность.
            НЕ комментируй стиль/форматирование. Только значимые находки. Пустая категория = пустой массив.
        """.trimIndent()
    }

    fun reviewPr(request: PrReviewRequest): PrReviewResponse {
        val diff = request.diff ?: projectGitService.getDiff(request.baseBranch)
        val files = request.changedFiles ?: projectGitService.getDiffFileNames(request.baseBranch)
        val title = request.prTitle ?: projectGitService.getLastCommitMessage() ?: "PR Review"
        val branch = projectGitService.getCurrentBranch()

        if (diff.isBlank()) {
            return PrReviewResponse(
                summary = "Нет изменений для ревью (diff пуст).",
                model = getModelName(),
                branch = branch
            )
        }

        val truncatedDiff = truncateDiff(diff)

        val ragContext = if (request.useRag) {
            try {
                val query = "$title ${files.joinToString(", ")}"
                val results = ragService.search(query, request.topK)
                log.info("qqwe_tag reviewPr RAG: query='{}', results={}", query, results.size)
                results.joinToString("\n\n") { it.text }
            } catch (e: Exception) {
                log.warn("qqwe_tag reviewPr RAG search failed: {}", e.message)
                ""
            }
        } else {
            ""
        }

        val userMessage = buildUserMessage(branch, title, ragContext, files, truncatedDiff)

        val messages = listOf(
            mapOf("role" to "system", "content" to SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to userMessage)
        )

        val modelName = getModelName()
        log.info("qqwe_tag reviewPr sending to LLM: model={}, branch={}, files={}", modelName, branch, files.size)

        val rawResponse = ollamaClient.chat(modelName, messages)
        log.info("qqwe_tag reviewPr LLM response length={}", rawResponse.length)

        return parseResponse(rawResponse, branch, ragContext.isNotBlank(), modelName)
    }

    private fun getModelName(): String = llmService.getActiveName() ?: DEFAULT_MODEL

    private fun buildUserMessage(
        branch: String?,
        title: String,
        ragContext: String,
        files: List<String>,
        diff: String
    ): String = buildString {
        appendLine("## Ветка: ${branch ?: "unknown"}")
        appendLine("## PR: $title")
        appendLine()
        if (ragContext.isNotBlank()) {
            appendLine("## Контекст из документации (RAG):")
            appendLine(ragContext)
        } else {
            appendLine("## Контекст из документации (RAG): не найден")
        }
        appendLine()
        appendLine("## Изменённые файлы:")
        files.forEach { appendLine("- $it") }
        appendLine()
        appendLine("## Diff:")
        appendLine("```diff")
        appendLine(diff)
        appendLine("```")
        appendLine()
        appendLine("Проанализируй этот diff и ответь в указанном JSON-формате.")
    }

    private fun truncateDiff(diff: String): String {
        if (diff.length <= MAX_DIFF_CHARS) return diff

        val fileDiffs = splitDiffByFiles(diff)
        val (codeDiffs, otherDiffs) = fileDiffs.partition { (name, _) ->
            val ext = name.substringAfterLast('.', "")
            ext in CODE_EXTENSIONS
        }

        val result = StringBuilder()
        var remaining = MAX_DIFF_CHARS
        var truncatedCount = 0

        for ((name, content) in codeDiffs + otherDiffs) {
            if (remaining <= 0) {
                truncatedCount++
                continue
            }
            val toAppend = if (content.length <= remaining) content else content.take(remaining) + "\n... (обрезано)"
            result.append(toAppend)
            remaining -= toAppend.length
        }

        if (truncatedCount > 0) {
            result.append("\n... и ещё $truncatedCount файл(ов) пропущено")
        }

        return result.toString()
    }

    private fun splitDiffByFiles(diff: String): List<Pair<String, String>> {
        val parts = diff.split("(?=diff --git)".toRegex()).filter { it.isNotBlank() }
        return parts.map { part ->
            val fileName = "diff --git a/(\\S+)".toRegex().find(part)?.groupValues?.get(1) ?: "unknown"
            fileName to part
        }
    }

    private fun parseResponse(
        raw: String,
        branch: String?,
        ragUsed: Boolean,
        model: String
    ): PrReviewResponse {
        val cleaned = raw
            .replace("```json", "").replace("```", "")
            .trim()

        return try {
            val jsonNode = objectMapper.readTree(cleaned)
            PrReviewResponse(
                summary = jsonNode.get("summary")?.asText() ?: raw,
                bugs = parseFindings(jsonNode.get("bugs")),
                architecturalIssues = parseFindings(jsonNode.get("architecturalIssues")),
                recommendations = parseFindings(jsonNode.get("recommendations")),
                branch = branch,
                ragContextUsed = ragUsed,
                model = model
            )
        } catch (e: Exception) {
            log.warn("qqwe_tag reviewPr JSON parse failed, wrapping raw text: {}", e.message)
            PrReviewResponse(
                summary = raw,
                branch = branch,
                ragContextUsed = ragUsed,
                model = model
            )
        }
    }

    private fun parseFindings(node: com.fasterxml.jackson.databind.JsonNode?): List<ReviewFinding> {
        if (node == null || !node.isArray) return emptyList()
        return node.mapNotNull { item ->
            try {
                ReviewFinding(
                    severity = item.get("severity")?.asText() ?: "info",
                    file = item.get("file")?.asText(),
                    line = item.get("line")?.asText(),
                    description = item.get("description")?.asText() ?: return@mapNotNull null
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
