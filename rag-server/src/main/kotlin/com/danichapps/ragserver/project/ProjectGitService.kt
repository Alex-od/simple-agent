package com.danichapps.ragserver.project

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class ProjectGitService(
    @Value("\${simpleagent.project-root:..}") projectRoot: String
) {

    private val log = LoggerFactory.getLogger(ProjectGitService::class.java)
    private val resolvedProjectRoot = File(projectRoot).absoluteFile.normalize()

    fun getProjectRoot(): String = resolvedProjectRoot.absolutePath

    fun isGitRepository(): Boolean =
        runGitCommand("rev-parse", "--is-inside-work-tree").trim().equals("true", ignoreCase = true)

    fun getCurrentBranch(): String? =
        runGitCommand("branch", "--show-current")
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }

    fun getChangedFiles(): List<String> =
        runGitCommand("status", "--short", "--untracked-files=no")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { statusLine ->
                statusLine.substringAfter(" ").trim()
            }
            .toList()

    fun getDiff(baseBranch: String = "master"): String =
        runGitCommand("diff", "$baseBranch...HEAD")

    fun getDiffFileNames(baseBranch: String = "master"): List<String> =
        runGitCommand("diff", "--name-only", "$baseBranch...HEAD")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

    fun getLastCommitMessage(): String? =
        runGitCommand("log", "-1", "--pretty=%s")
            .trim()
            .takeIf { it.isNotBlank() }

    private fun runGitCommand(vararg args: String): String {
        val process = ProcessBuilder(listOf("git.exe") + args)
            .directory(resolvedProjectRoot)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            log.warn("git.exe timed out for args={}", args.toList())
            return ""
        }

        if (process.exitValue() != 0) {
            log.warn("git.exe failed for args={}, exitCode={}, output={}", args.toList(), process.exitValue(), output)
            return ""
        }

        return output
    }
}
