package com.danichapps.simpleagent.data.local

import android.content.Context
import android.net.Uri

private const val PREFS_NAME = "rag_folder_prefs"
private const val KEY_TREE_URI = "tree_uri"
private const val KEY_DISPLAY_NAME = "display_name"

class RagFolderPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri, displayName: String?) {
        prefs.edit()
            .putString(KEY_TREE_URI, uri.toString())
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun getTreeUri(): Uri? = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun getTreeUriString(): String? = prefs.getString(KEY_TREE_URI, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun hasFolder(): Boolean = getTreeUri() != null
}
