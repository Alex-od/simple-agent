package com.danichapps.simpleagent.data.remote

import android.content.Context
import com.danichapps.simpleagent.data.local.ModelSelectionManager
import com.danichapps.simpleagent.BuildConfig
import java.io.File

private const val DEFAULT_ON_DEVICE_MODEL_DIR = "/storage/50D5-2CE6/agents"

class DeviceModelPathResolver(
    private val context: Context,
    private val modelSelectionManager: ModelSelectionManager
) {
    fun resolveModelFile(): File {
        val configuredPath = BuildConfig.ON_DEVICE_LLM_MODEL_PATH.trim()
        if (configuredPath.isNotEmpty()) return File(configuredPath)

        modelSelectionManager.getSelectedModelFile()?.let { return it }

        val appBaseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val appScopedModel = File(File(appBaseDir, "llm"), BuildConfig.ON_DEVICE_LLM_MODEL_FILENAME)
        if (appScopedModel.exists()) {
            return appScopedModel
        }

        val preferredDir = File(DEFAULT_ON_DEVICE_MODEL_DIR)
        return File(preferredDir, BuildConfig.ON_DEVICE_LLM_MODEL_FILENAME)
    }
}
