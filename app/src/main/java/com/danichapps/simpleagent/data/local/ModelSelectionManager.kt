package com.danichapps.simpleagent.data.local

import android.content.Context
import android.net.Uri
import java.io.File

private const val PREFS_NAME = "model_selection_prefs"
private const val KEY_MODEL_FILE_NAME = "model_file_name"
private const val KEY_MODEL_DISPLAY_NAME = "model_display_name"

class ModelSelectionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedModelDisplayName(): String? = prefs.getString(KEY_MODEL_DISPLAY_NAME, null)

    fun getSelectedModelFile(): File? {
        val fileName = prefs.getString(KEY_MODEL_FILE_NAME, null) ?: return null
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(File(baseDir, "llm"), fileName)
        return file.takeIf { it.exists() }
    }

    suspend fun importModel(uri: Uri, displayName: String?): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val rawName = (displayName ?: "selected-model.gguf").substringAfterLast('/')
        val safeName = if (rawName.endsWith(".gguf", ignoreCase = true)) rawName else "$rawName.gguf"
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val targetDir = File(baseDir, "llm").apply { mkdirs() }
        val targetFile = File(targetDir, safeName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Не удалось открыть выбранный файл модели")

        prefs.edit()
            .putString(KEY_MODEL_FILE_NAME, safeName)
            .putString(KEY_MODEL_DISPLAY_NAME, displayName ?: safeName)
            .apply()

        targetFile
    }
}
