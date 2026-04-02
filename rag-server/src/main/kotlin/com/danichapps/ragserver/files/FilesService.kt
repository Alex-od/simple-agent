package com.danichapps.ragserver.files

import com.danichapps.ragserver.files.dto.FilesRequest
import com.danichapps.ragserver.files.dto.FilesResponse
import com.danichapps.ragserver.llm.LlmService
import com.danichapps.ragserver.llm.OllamaClient
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

private const val DEFAULT_MODEL = "qwen2.5:7b"
private const val MAX_ITERATIONS = 6
private const val MAX_FILE_CHARS = 4000
private const val MAX_SEARCH_MATCHES = 30
private val EXCLUDED_DIRS = setOf("build", ".gradle", ".kotlin", ".git", "__pycache__", "node_modules")
private val SOURCE_EXTENSIONS = setOf("kt", "py", "java", "xml", "md", "json", "yml", "yaml")

@Service
class FilesService(
    private val ollamaClient: OllamaClient,
    private val llmService: LlmService,
    @Value("\${simpleagent.project-root:..}") projectRoot: String
) {

    private val log = LoggerFactory.getLogger(FilesService::class.java)
    private val root: File = File(projectRoot).absoluteFile.normalize()

    fun analyze(request: FilesRequest): FilesResponse {
        val model = llmService.getActiveName() ?: DEFAULT_MODEL
        val opLog = mutableListOf<String>()

        log.info("qqwe_tag FilesService analyze: task='{}', model={}", request.task, model)

        val messages = mutableListOf<Map<String, Any>>(
            mapOf("role" to "system", "content" to AGENT_SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to request.task)
        )

        var finalAnswer = ""
        var iterations = 0

        while (iterations < MAX_ITERATIONS) {
            iterations++
            log.debug("qqwe_tag FilesService, agentLoop: iteration={}", iterations)

            val response = try {
                ollamaClient.chatWithTools(model, messages, buildToolsDefinition())
            } catch (e: Exception) {
                log.warn("qqwe_tag FilesService, agentLoop: Ollama error: {}", e.message)
                opLog.add("❌ Ошибка LLM: ${e.message?.take(80)}")
                break
            }

            if (!response.hasToolCalls) {
                finalAnswer = response.content
                log.info("qqwe_tag FilesService, agentLoop: final answer at iteration={}, len={}", iterations, finalAnswer.length)
                break
            }

            // Добавляем сообщение ассистента с tool_calls в историю
            messages.add(
                mapOf(
                    "role" to "assistant",
                    "content" to response.content,
                    "tool_calls" to response.toolCalls.map { tc ->
                        mapOf("function" to mapOf("name" to tc.name, "arguments" to tc.arguments))
                    }
                )
            )

            // Выполняем каждый tool call
            for (toolCall in response.toolCalls) {
                val toolName = toolCall.name
                val args = toolCall.arguments
                log.info("qqwe_tag FilesService, executeTool: name={}, args={}", toolName, args.toString().take(100))

                val result = try {
                    executeTool(toolName, args)
                } catch (e: Exception) {
                    "Tool error: ${e.message}"
                }

                val logEntry = "🔧 $toolName(${summarizeArgs(args)}) → ${result.take(60)}…"
                opLog.add(logEntry)

                messages.add(mapOf("role" to "tool", "content" to result))
            }
        }

        if (finalAnswer.isBlank() && iterations >= MAX_ITERATIONS) {
            finalAnswer = "Достигнут лимит итераций ($MAX_ITERATIONS). Последний контекст:\n" +
                messages.lastOrNull { it["role"] == "tool" }?.get("content")?.toString()?.take(500).orEmpty()
            opLog.add("⚠️ Лимит итераций достигнут")
        }

        log.info("qqwe_tag FilesService analyze: done, ops={}, resultLen={}", opLog.size, finalAnswer.length)
        return FilesResponse(result = finalAnswer, operationLog = opLog, model = model)
    }

    // ── Tool execution ───────────────────────────────────────────────────────

    private fun executeTool(name: String, args: JsonNode): String = when (name) {
        "list_files" -> {
            val pattern = args.get("pattern")?.asText() ?: "**/*.kt"
            listFiles(pattern)
        }
        "read_file" -> {
            val path = args.get("path")?.asText() ?: return "Error: missing path"
            val maxLines = args.get("max_lines")?.asInt() ?: 150
            readFile(path, maxLines)
        }
        "search_in_files" -> {
            val query = args.get("query")?.asText() ?: return "Error: missing query"
            val exts = args.get("extensions")?.let { node ->
                if (node.isArray) node.map { it.asText().trimStart('.') }.toSet()
                else SOURCE_EXTENSIONS
            } ?: SOURCE_EXTENSIONS
            searchInFiles(query, exts)
        }
        "write_file" -> {
            val path = args.get("path")?.asText() ?: return "Error: missing path"
            val content = args.get("content")?.asText() ?: return "Error: missing content"
            writeFile(path, content)
        }
        else -> "Unknown tool: $name"
    }

    // ── File operations ──────────────────────────────────────────────────────

    private fun listFiles(pattern: String): String {
        val files = mutableListOf<String>()
        try {
            root.walkTopDown()
                .onEnter { dir -> dir.name !in EXCLUDED_DIRS }
                .filter { it.isFile }
                .forEach { file ->
                    val rel = file.relativeTo(root).path.replace('\\', '/')
                    if (matchesGlob(rel, pattern)) files.add(rel)
                }
        } catch (e: Exception) {
            return "Error listing files: ${e.message}"
        }
        files.sort()
        return if (files.isEmpty()) "No files found for pattern: $pattern"
        else "Files (${files.size}):\n${files.take(60).joinToString("\n")}"
    }

    private fun readFile(relPath: String, maxLines: Int): String {
        val target = root.resolve(relPath).normalize()
        if (!target.canonicalPath.startsWith(root.canonicalPath)) return "Access denied"
        if (!target.exists()) return "File not found: $relPath"
        return try {
            val lines = target.readLines(Charsets.UTF_8)
            val result = lines.take(maxLines).joinToString("\n")
            if (lines.size > maxLines) "$result\n... [truncated ${lines.size - maxLines} lines]"
            else result
        } catch (e: Exception) {
            "Error reading $relPath: ${e.message}"
        }
    }

    private fun searchInFiles(query: String, extensions: Set<String>): String {
        val matches = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { dir -> dir.name !in EXCLUDED_DIRS }
            .filter { it.isFile && it.extension in extensions }
            .forEach { file ->
                if (matches.size >= MAX_SEARCH_MATCHES) return@forEach
                try {
                    file.readLines(Charsets.UTF_8).forEachIndexed { idx, line ->
                        if (matches.size < MAX_SEARCH_MATCHES && query.lowercase() in line.lowercase()) {
                            val rel = file.relativeTo(root).path.replace('\\', '/')
                            matches.add("$rel:${idx + 1}: ${line.trim()}")
                        }
                    }
                } catch (_: Exception) {}
            }
        return if (matches.isEmpty()) "No matches for '$query'"
        else "Found ${matches.size} match(es) for '$query':\n${matches.joinToString("\n")}"
    }

    private fun writeFile(relPath: String, content: String): String {
        val target = root.resolve(relPath).normalize()
        if (!target.canonicalPath.startsWith(root.canonicalPath)) return "Access denied"
        return try {
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
            "Written: $relPath (${content.length} chars)"
        } catch (e: Exception) {
            "Error writing $relPath: ${e.message}"
        }
    }

    // ── Tools definition ─────────────────────────────────────────────────────

    private fun buildToolsDefinition(): List<Map<String, Any>> = listOf(
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "list_files",
                "description" to "List project files matching a glob pattern. Use to discover what files exist before reading them.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "pattern" to mapOf("type" to "string", "description" to "Glob pattern, e.g. **/*.kt, app/**/*.kt, rag-server/**/*.kt")
                    ),
                    "required" to listOf("pattern")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "read_file",
                "description" to "Read content of a specific project file by relative path.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "path" to mapOf("type" to "string", "description" to "Relative file path, e.g. app/src/main/java/com/danichapps/simpleagent/di/AppModule.kt"),
                        "max_lines" to mapOf("type" to "integer", "description" to "Max lines to return (default 150)")
                    ),
                    "required" to listOf("path")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "search_in_files",
                "description" to "Search for a text query across all project files. Returns file:line:text matches.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "query" to mapOf("type" to "string", "description" to "Text to search (case-insensitive)"),
                        "extensions" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                            "description" to "File extensions to search, e.g. [\"kt\", \"py\"]. Default: all source files"
                        )
                    ),
                    "required" to listOf("query")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "write_file",
                "description" to "Create or overwrite a file in the project. Use to save generated documentation, reports, or new files.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "path" to mapOf("type" to "string", "description" to "Relative file path to write"),
                        "content" to mapOf("type" to "string", "description" to "Full file content")
                    ),
                    "required" to listOf("path", "content")
                )
            )
        )
    )

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun matchesGlob(path: String, pattern: String): Boolean = try {
        val normalizedPath = path.replace('\\', '/')
        val normalizedPattern = pattern.replace('\\', '/')
        val matcher = FileSystems.getDefault().getPathMatcher("glob:$normalizedPattern")
        matcher.matches(Paths.get(normalizedPath))
    } catch (_: Exception) {
        // fallback: simple contains check if pattern is not a valid glob
        path.contains(pattern.trimStart('*', '/'))
    }

    private fun summarizeArgs(args: JsonNode): String =
        args.fields().asSequence().take(2).joinToString(", ") { "${it.key}=${it.value.asText().take(30)}" }

    companion object {
        private val AGENT_SYSTEM_PROMPT = """
            You are a file assistant for the SimpleAgent Android project (Kotlin, Android + Spring Boot).
            Project root contains: app/ (Android), rag-server/ (Spring Boot backend), tools/ (MCP server).

            You have tools:
            - list_files(pattern): discover files, e.g. pattern="app/**/*.kt" or "rag-server/**/*.kt"
            - read_file(path): read a file using FULL relative path like "app/src/main/java/com/danichapps/simpleagent/di/AppModule.kt"
            - search_in_files(query): grep across project files
            - write_file(path, content): create or update a file

            IMPORTANT rules:
            1. Always use tools FIRST — never answer from memory alone.
            2. Use list_files to discover, then read_file with the FULL path exactly as returned by list_files.
            3. NEVER truncate or shorten file paths — use them verbatim.
            4. For architecture checks: search imports, then read suspicious files.
            5. For docs generation: read key files, then write_file to save result.
            6. Answer in Russian. Reference exact file:line in your findings.
        """.trimIndent()
    }
}
