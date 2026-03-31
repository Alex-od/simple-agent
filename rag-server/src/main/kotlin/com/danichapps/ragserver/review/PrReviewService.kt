package com.danichapps.ragserver.review

import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import com.danichapps.ragserver.project.ProjectGitService
import com.danichapps.ragserver.review.dto.PrReviewRequest
import com.danichapps.ragserver.review.dto.PrReviewResponse
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
        private const val MAX_DIFF_CHARS = 6_000
        private const val DEFAULT_MODEL = "llama3.2:3b"
        private val CODE_EXTENSIONS = setOf("kt", "java", "yml", "gradle", "kts", "xml")

        private val SYSTEM_PROMPT = """
            You are a Kotlin/Android code reviewer. Look at the git diff and fill in this JSON template.
            Return ONLY the JSON, no other text.

            Template (fill in the arrays with your findings, or leave empty):
            {"summary":"one sentence about the changes","bugs":["[Foo.kt:42] possible NPE on x!!"],"architecturalIssues":["[Bar.kt] business logic belongs in use case, not ViewModel"],"recommendations":["[Baz.kt] extract duplicated validation into helper"]}

            Rules:
            - bugs: actual code errors (NPE, leaks, crashes, wrong coroutine scope)
            - architecturalIssues: wrong layer (UI doing IO, data class with logic, etc.)
            - recommendations: improvements, but not bugs
            - Each item is a plain string like "[FileName.kt:line] description"
            - Empty category = []
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
                val results = ragService.search(query, minOf(request.topK, 2))
                log.info("qqwe_tag reviewPr RAG: query='{}', results={}", query, results.size)
                results.joinToString("\n\n") { it.text }.take(500)
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
        val cleaned = raw.replace("```json", "").replace("```", "").trim()
        val jsonStr = extractJsonObject(cleaned)

        return try {
            val jsonNode = objectMapper.readTree(jsonStr)
            PrReviewResponse(
                summary = jsonNode.get("summary")?.asText() ?: "",
                bugs = parseStringArray(jsonNode.get("bugs")),
                architecturalIssues = parseStringArray(jsonNode.get("architecturalIssues")),
                recommendations = parseStringArray(jsonNode.get("recommendations")),
                branch = branch,
                ragContextUsed = ragUsed,
                model = model
            )
        } catch (e: Exception) {
            log.warn("qqwe_tag reviewPr JSON parse failed: {}", e.message)
            PrReviewResponse(
                summary = raw,
                branch = branch,
                ragContextUsed = ragUsed,
                model = model
            )
        }
    }

    /** Находит первый JSON-объект в тексте, обрезанный JSON дополняет до валидного */
    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        if (start == -1) return text
        var open = 0
        val sb = StringBuilder()
        for (ch in text.substring(start)) {
            sb.append(ch)
            if (ch == '{') open++
            else if (ch == '}') { open--; if (open == 0) return sb.toString() }
        }
        // JSON обрезан — закрываем незакрытые скобки
        repeat(open) { sb.append('}') }
        return sb.toString()
    }

    private fun parseStringArray(node: com.fasterxml.jackson.databind.JsonNode?): List<String> {
        if (node == null || !node.isArray) return emptyList()
        return node.mapNotNull { it.asText().takeIf { s -> s.isNotBlank() } }
    }
}
