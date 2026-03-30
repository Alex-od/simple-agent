package com.danichapps.simpleagent.data

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var isRagEnabled: Boolean
        get() = prefs.getBoolean("rag_enabled", false)
        set(value) = prefs.edit().putBoolean("rag_enabled", value).apply()

    var isOfflineMode: Boolean
        get() = prefs.getBoolean("offline_mode", false)
        set(value) = prefs.edit().putBoolean("offline_mode", value).apply()
}
