package com.danichapps.simpleagent.data.local

import android.content.Context
import com.danichapps.simpleagent.domain.model.ChatMode

private const val PREFS_NAME = "chat_mode_prefs"
private const val KEY_CHAT_MODE = "chat_mode"

class ChatModeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ChatMode {
        val saved = prefs.getString(KEY_CHAT_MODE, ChatMode.OPENAI.name)
        return runCatching { ChatMode.valueOf(saved ?: ChatMode.OPENAI.name) }
            .getOrDefault(ChatMode.OPENAI)
    }

    fun save(mode: ChatMode) {
        prefs.edit().putString(KEY_CHAT_MODE, mode.name).apply()
    }
}
