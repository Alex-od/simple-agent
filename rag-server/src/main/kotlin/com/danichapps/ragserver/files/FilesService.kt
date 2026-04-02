package com.danichapps.ragserver.files

import com.danichapps.ragserver.files.dto.FilesRequest
import com.danichapps.ragserver.files.dto.FilesResponse
import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

private const val DEFAULT_MODEL = "llama3.2:3b"
private const val MAX_FILE_CHARS = 3000
private const val MAX_SEARCH_MATCHES = 40
private val EXCLUDED_DIRS = setOf("build", ".gradle", ".kotlin", ".git", "__pycache__", "node_modules")
private val SOURCE_EXTENSIONS = setOf(".kt", ".py", ".java", ".xml", ".md", ".json", ".yml", ".yaml")

@Service
class FilesService(
    private val ollamaClient: OllamaClient,
    private val llmService: LlmService,
    @Value("\${simpleagent.project-root:..}") projectRoot: String
) {

    private val log = LoggerFactory.getLogger(FilesService::class.java)
    private val root: File = File(projectRoot).absoluteFile.normalize()

    fun analyze(request: FilesRequest): FilesResponse {
        val task = request.task.trim()
        val log2 = mutableListOf<String>()
        val model = llmService.getActiveName() ?: DEFAULT_MODEL

        log.info("qqwe_tag FilesService analyze: task='{}', model={}", task, model)

        val result = when {
            isSearchIntent(task) -> handleSearch(task, model, log2)
            isGenerateIntent(task) -> handleGenerate(task, model, log2)
            else -> handleGeneral(task, model, log2)
        }

        log.info(
            "qqwe_tag FilesService analyze: done, ops={}, resultLen={}",
            log2.size, result.length
        )

        return FilesResponse(result = result, operationLog = log2, model = model)
    }

    // ── Сценарий 1: Поиск usages ────────────────────────────────────────────

    private fun handleSearch(task: String, model: String, opLog: MutableList<String>): String {
        val query = extractSearchQuery(task)
        opLog.add("🔍 Поиск: \"$query\"")

        val matches = searchInFiles(query)
        val matchCount = matches.size
        val fileCount = matches.map { it.file }.distinct().size

        opLog.add("📂 Найдено: $matchCount вхождений в $fileCount файл(ах)")

        if (matches.isEmpty()) {
            return "Вхождений для «$query» не найдено."
        }

        val context = matches.joinToString("\n") { "${it.file}:${it.line}: ${it.text}" }
            .take(2000)

        val prompt = buildString {
            appendLine("Задача: $task")
            appendLine()
            appendLine("Найденные вхождения (файл:строка: код):")
            appendLine(context)
            appendLine()
            appendLine("Проанализируй: где и как используется «$query», есть ли паттерны или проблемы.")
            appendLine("Ответь на русском, кратко и структурировано.")
        }

        return ollamaClient.chat(model, listOf(
            mapOf("role" to "system", "content" to FILE_SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to prompt)
        ))
    }

    // ── Сценарий 2: Генерация README / ADR ──────────────────────────────────

    private fun handleGenerate(task: String, model: String, opLog: MutableList<String>): String {
        val outputPath = detectOutputPath(task)
        opLog.add("📄 Целевой файл: $outputPath")

        val keyFiles = collectKeyFiles()
        opLog.add("📂 Прочитано: ${keyFiles.size} ключевых файлов")

        val filesSummary = keyFiles.joinToString("\n\n") { (path, content) ->
            "### $path\n${content.take(MAX_FILE_CHARS)}"
        }.take(6000)

        val prompt = buildString {
            appendLine("Задача: $task")
            appendLine()
            appendLine("Ключевые файлы проекта:")
            appendLine(filesSummary)
            appendLine()
            appendLine("Сгенерируй файл \"$outputPath\" на основе реального кода.")
            appendLine("Отвечай только содержимым файла, без лишних пояснений.")
        }

        val generated = ollamaClient.chat(model, listOf(
            mapOf("role" to "system", "content" to FILE_SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to prompt)
        ))

        val outFile = root.resolve(outputPath)
        outFile.parentFile?.mkdirs()
        outFile.writeText(generated, Charsets.UTF_8)
        opLog.add("✅ Создано: $outputPath (${generated.length} символов)")

        return generated
    }

    // ── Общий сценарий ───────────────────────────────────────────────────────

    private fun handleGeneral(task: String, model: String, opLog: MutableList<String>): String {
        val gitStatus = runGit("status", "--short")
        opLog.add("📋 git status получен")
        val prompt = buildString {
            appendLine("Задача по проекту: $task")
            appendLine()
            appendLine("Git status:")
            appendLine(gitStatus.take(1000))
            appendLine()
            appendLine("Ответь на русском языке.")
        }
        return ollamaClient.chat(model, listOf(
            mapOf("role" to "system", "content" to FILE_SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to prompt)
        ))
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    private fun isSearchIntent(task: String): Boolean {
        val t = task.lowercase()
        return listOf("найди", "поищи", "где используется", "usages", "usage", "найти", "поиск", "where is").any { it in t }
    }

    private fun isGenerateIntent(task: String): Boolean {
        val t = task.lowercase()
        return listOf("сгенерируй", "создай", "генерация", "generate", "readme", "adr", "changelog", "обнови документацию").any { it in t }
    }

    private fun extractSearchQuery(task: String): String {
        val stopWords = setOf(
            "найди", "поищи", "где", "используется", "все", "места", "использования",
            "usages", "usage", "найти", "поиск", "всех", "файлах", "проекте", "where", "is", "find", "all"
        )
        return task.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.lowercase() !in stopWords }
            .joinToString(" ")
            .trim()
            .ifBlank { task }
    }

    private fun detectOutputPath(task: String): String {
        val lower = task.lowercase()
        return when {
            "adr" in lower -> "docs/adr/adr-${System.currentTimeMillis() / 1000}.md"
            "changelog" in lower -> "CHANGELOG.md"
            else -> "README.md"
        }
    }

    data class SearchMatch(val file: String, val line: Int, val text: String)

    private fun searchInFiles(query: String): List<SearchMatch> {
        val results = mutableListOf<SearchMatch>()
        root.walkTopDown()
            .onEnter { dir -> dir.name !in EXCLUDED_DIRS }
            .filter { it.isFile && it.extension in SOURCE_EXTENSIONS.map { e -> e.trimStart('.') } }
            .forEach { file ->
                if (results.size >= MAX_SEARCH_MATCHES) return@forEach
                try {
                    file.readLines(Charsets.UTF_8).forEachIndexed { idx, line ->
                        if (results.size < MAX_SEARCH_MATCHES && query.lowercase() in line.lowercase()) {
                            val rel = file.relativeTo(root).path.replace('\\', '/')
                            results.add(SearchMatch(rel, idx + 1, line.trim()))
                        }
                    }
                } catch (_: Exception) {}
            }
        return results
    }

    private fun collectKeyFiles(): List<Pair<String, String>> {
        val priority = listOf(
            "README.md", "CLAUDE.md",
            "app/src/main/java/com/danichapps/simpleagent/di/AppModule.kt",
            "app/src/main/java/com/danichapps/simpleagent/domain/usecase/CommandRouterUseCase.kt",
            "rag-server/src/main/kotlin/com/danichapps/ragserver/RagServerApplication.kt",
            "tools/project_mcp_server.py"
        )
        val result = mutableListOf<Pair<String, String>>()
        priority.forEach { rel ->
            val f = root.resolve(rel)
            if (f.exists() && f.isFile) {
                try {
                    result.add(rel to f.readText(Charsets.UTF_8))
                } catch (_: Exception) {}
            }
        }
        // добавляем Controller-файлы для контекста
        root.walkTopDown()
            .onEnter { dir -> dir.name !in EXCLUDED_DIRS }
            .filter { it.isFile && it.name.endsWith("Controller.kt") }
            .take(6)
            .forEach { f ->
                val rel = f.relativeTo(root).path.replace('\\', '/')
                if (result.none { it.first == rel }) {
                    try { result.add(rel to f.readText(Charsets.UTF_8)) } catch (_: Exception) {}
                }
            }
        return result
    }

    private fun runGit(vararg args: String): String = try {
        val proc = ProcessBuilder("git", *args)
            .directory(root)
            .redirectErrorStream(true)
            .start()
        proc.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
    } catch (e: Exception) {
        "git error: ${e.message}"
    }

    companion object {
        private val FILE_SYSTEM_PROMPT = """
            Ты — AI-ассистент для работы с кодовой базой Android-проекта SimpleAgent.
            Анализируй код вдумчиво, отвечай на русском языке.
            При поиске usages — указывай файл, строку и назначение.
            При генерации документации — следуй реальной структуре кода.
        """.trimIndent()
    }
}
