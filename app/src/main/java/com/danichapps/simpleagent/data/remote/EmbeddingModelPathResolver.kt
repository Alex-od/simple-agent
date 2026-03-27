package com.danichapps.simpleagent.data.remote

import android.content.Context
import com.danichapps.simpleagent.BuildConfig
import com.danichapps.simpleagent.data.local.EmbeddingModelSelectionManager
import java.io.File

class EmbeddingModelPathResolver(
    private val context: Context,
    private val embeddingModelSelectionManager: EmbeddingModelSelectionManager
) {
    fun resolveModelFile(): File? {
        val configuredPath = BuildConfig.LOCAL_EMBEDDING_MODEL_PATH.trim()
        if (configuredPath.isNotEmpty()) {
            val configuredFile = File(configuredPath)
            if (configuredFile.exists()) return configuredFile
        }

        embeddingModelSelectionManager.getSelectedModelFile()?.let { return it }

        val configuredName = BuildConfig.LOCAL_EMBEDDING_MODEL_FILENAME.trim()
        if (configuredName.isEmpty()) return null

        val appBaseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val appScopedModel = File(File(appBaseDir, "embeddings"), configuredName)
        return appScopedModel.takeIf { it.exists() }
    }
}
