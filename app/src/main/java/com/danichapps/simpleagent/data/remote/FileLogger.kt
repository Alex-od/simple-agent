package com.danichapps.simpleagent.data.remote

import android.content.Context
import android.util.Log
import io.ktor.client.plugins.logging.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val KTOR_LOG_TAG = "qqwe_tag KtorFileLogger"

class FileLogger(context: Context) : Logger {

    private val logsDirectory = File(context.filesDir, "logs").apply { mkdirs() }
    private val logFile: File = File(logsDirectory, "ktor.log")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun log(message: String) {
        appendLine(message)
    }

    fun logEvent(message: String) {
        appendLine("[event] $message")
    }

    private fun appendLine(message: String) {
        runCatching {
            val line = "[${dateFormat.format(Date())}] $message\n"
            synchronized(logFile) {
                logFile.appendText(line)
            }
        }.onFailure { error ->
            Log.e(KTOR_LOG_TAG, "Failed to write Ktor log file", error)
        }
    }
}
