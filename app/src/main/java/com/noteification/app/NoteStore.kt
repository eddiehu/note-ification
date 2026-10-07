package com.noteification.app

import android.content.Context

/** The one and only note, persisted in SharedPreferences. */
class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("note", Context.MODE_PRIVATE)

    fun load(): String = prefs.getString(KEY_TEXT, "") ?: ""

    fun save(text: String) {
        prefs.edit().putString(KEY_TEXT, text).apply()
    }

    companion object {
        private const val KEY_TEXT = "text"
    }
}
