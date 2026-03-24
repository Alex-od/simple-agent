package com.danichapps.simpleagent.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Модель скачивается вручную через adb push в папку external files dir приложения.
// HuggingFace: https://huggingface.co/litert-community/Gemma3-1B-IT → gemma3-1b-it-int4.task
private const val MODEL_URL = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"
private const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
private const val PREFS_NAME = "model_download_prefs"
private const val PREF_STORAGE_PATH = "selected_storage_path"

sealed class DownloadState {
    data class Downloading(val progress: Int, val downloadedMb: Int, val totalMb: Int) : DownloadState()
    object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class StorageOption(
    val label: String,
    val path: String,
    val freeSpaceGb: Float
)

class ModelDownloadManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStorageOptions(): List<StorageOption> {
        val options = mutableListOf<StorageOption>()

        // Внешняя папка приложения (доступна через adb без root)
        val externalAppDir = context.getExternalFilesDir(null)
        if (externalAppDir != null) {
            options.add(
                StorageOption(
                    label = "Внешняя память (рекомендуется)",
                    path = externalAppDir.absolutePath,
                    freeSpaceGb = externalAppDir.freeSpace / (1024f * 1024f * 1024f)
                )
            )
        }

        // Внутренняя память приложения
        val internal = context.filesDir
        options.add(
            StorageOption(
                label = "Внутренняя память",
                path = internal.absolutePath,
                freeSpaceGb = internal.freeSpace / (1024f * 1024f * 1024f)
            )
        )

        // SD-карта (если есть — второй элемент getExternalFilesDirs)
        val externalDirs = context.getExternalFilesDirs(null)
        if (externalDirs.size > 1) {
            val sd = externalDirs[1]
            if (sd != null) {
                options.add(
                    StorageOption(
                        label = "SD-карта",
                        path = sd.absolutePath,
                        freeSpaceGb = sd.freeSpace / (1024f * 1024f * 1024f)
                    )
                )
            }
        }

        return options
    }

    private fun defaultStoragePath(): String =
        context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath

    var selectedStoragePath: String
        get() = prefs.getString(PREF_STORAGE_PATH, defaultStoragePath())!!
        set(value) = prefs.edit().putString(PREF_STORAGE_PATH, value).apply()

    val modelPath: String
        get() = File(selectedStoragePath, MODEL_FILENAME).absolutePath

    fun isModelDownloaded(): Boolean = File(modelPath).exists()

    fun downloadModel(targetDir: String): Flow<DownloadState> = flow {
        val targetFile = File(targetDir, MODEL_FILENAME)
        val partialFile = File(targetDir, "$MODEL_FILENAME.part")

        try {
            withContext(Dispatchers.IO) {
                val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    // HTTP Range resume
                    val downloaded = if (partialFile.exists()) partialFile.length() else 0L
                    if (downloaded > 0) setRequestProperty("Range", "bytes=$downloaded-")
                    connect()
                }

                val responseCode = connection.responseCode
                val append = responseCode == HttpURLConnection.HTTP_PARTIAL
                val totalLength = if (append) {
                    connection.getHeaderField("Content-Range")
                        ?.substringAfterLast('/')?.toLongOrNull()
                        ?: connection.contentLengthLong
                } else {
                    connection.contentLengthLong
                }
                val alreadyDownloaded = if (append) partialFile.length() else 0L

                connection.inputStream.use { input ->
                    FileOutputStream(partialFile, append).buffered().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = alreadyDownloaded
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (totalLength > 0) ((totalRead * 100) / totalLength).toInt() else 0
                            val downloadedMb = (totalRead / (1024 * 1024)).toInt()
                            val totalMb = (totalLength / (1024 * 1024)).toInt()
                            emit(DownloadState.Downloading(progress, downloadedMb, totalMb))
                        }
                    }
                }
                connection.disconnect()

                // Переименовываем после полной загрузки
                partialFile.renameTo(targetFile)
                selectedStoragePath = targetDir
            }
            emit(DownloadState.Done)
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Ошибка скачивания"))
        }
    }
}
