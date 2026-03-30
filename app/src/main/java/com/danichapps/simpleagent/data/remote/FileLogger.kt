package com.danichapps.simpleagent.data.remote

import android.content.Context
import io.ktor.client.plugins.logging.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogger(context: Context) : Logger {

    private val logFile: File = File(context.getExternalFilesDir(null), "ktor.log")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun log(message: String) {
        val line = "[${dateFormat.format(Date())}] $message\n"
        logFile.appendText(line)
    }
}
