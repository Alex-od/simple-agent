package com.danichapps.simpleagent.data.local

import android.content.Context
import com.danichapps.simpleagent.BuildConfig
import com.danichapps.simpleagent.domain.model.LocalServerSettings

private const val PREFS_NAME = "local_server_settings"
private const val KEY_BASE_URL = "base_url"
private const val KEY_MODEL = "model"

class LocalServerSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LocalServerSettings = LocalServerSettings(
        baseUrl = prefs.getString(KEY_BASE_URL, BuildConfig.LOCAL_SERVER_BASE_URL).orEmpty().normalizeBaseUrl(),
        model = prefs.getString(KEY_MODEL, BuildConfig.LOCAL_SERVER_MODEL).orEmpty()
    )

    fun save(settings: LocalServerSettings) {
        prefs.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.normalizeBaseUrl())
            .putString(KEY_MODEL, settings.model.trim())
            .apply()
    }

    private fun String.normalizeBaseUrl(): String {
        val normalized = trim().trimEnd('/')
        if (normalized.isEmpty()) return normalized
        return if (normalized.endsWith("/v1")) normalized else "$normalized/v1"
    }
}
