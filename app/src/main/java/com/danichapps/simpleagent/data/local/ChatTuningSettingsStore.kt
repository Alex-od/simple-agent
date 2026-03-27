package com.danichapps.simpleagent.data.local

import android.content.Context
import com.danichapps.simpleagent.domain.model.ChatTuningSettings

private const val PREFS_NAME = "chat_tuning_settings"
private const val KEY_TEMPERATURE = "temperature"
private const val KEY_MAX_TOKENS = "max_tokens"
private const val KEY_SYSTEM_PROMPT = "system_prompt"

class ChatTuningSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ChatTuningSettings = ChatTuningSettings(
        temperature = prefs.getFloat(KEY_TEMPERATURE, 0.2f).coerceIn(0f, 2f),
        maxTokens = prefs.getInt(KEY_MAX_TOKENS, 64).coerceIn(16, 512),
        systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "").orEmpty()
    )

    fun save(settings: ChatTuningSettings) {
        prefs.edit()
            .putFloat(KEY_TEMPERATURE, settings.temperature.coerceIn(0f, 2f))
            .putInt(KEY_MAX_TOKENS, settings.maxTokens.coerceIn(16, 512))
            .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
            .apply()
    }
}
