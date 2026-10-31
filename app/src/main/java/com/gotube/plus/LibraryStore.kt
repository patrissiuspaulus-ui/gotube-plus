package com.gotube.plus

import android.content.Context

class LibraryStore(context: Context) {
    private val preferences = context.getSharedPreferences("gotube_plus", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = preferences.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()

    fun toggleFavorite(id: String): Set<String> {
        val updated = favorites().toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        preferences.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return updated
    }

    fun history(): List<String> = preferences.getString(KEY_HISTORY, "")
        .orEmpty()
        .split('|')
        .filter(String::isNotBlank)

    fun addToHistory(id: String) {
        val updated = (listOf(id) + history().filterNot { it == id }).take(MAX_HISTORY)
        preferences.edit().putString(KEY_HISTORY, updated.joinToString("|")).apply()
    }

    fun isDarkMode(): Boolean = preferences.getBoolean(KEY_DARK_MODE, true)

    fun setDarkMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun pictureInPictureEnabled(): Boolean = preferences.getBoolean(KEY_PIP, true)

    fun setPictureInPictureEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PIP, enabled).apply()
    }

    fun savedPosition(): Pair<String, Long>? {
        val id = preferences.getString(KEY_MEDIA_ID, null) ?: return null
        return id to preferences.getLong(KEY_POSITION, 0L)
    }

    fun savePlayback(id: String?, position: Long, playing: Boolean) {
        preferences.edit()
            .putString(KEY_MEDIA_ID, id)
            .putLong(KEY_POSITION, position.coerceAtLeast(0L))
            .putBoolean(KEY_WAS_PLAYING, playing)
            .apply()
    }

    fun wasPlaying(): Boolean = preferences.getBoolean(KEY_WAS_PLAYING, false)

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_HISTORY = "history"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_PIP = "picture_in_picture"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_POSITION = "position"
        const val KEY_WAS_PLAYING = "was_playing"
        const val MAX_HISTORY = 40
    }
}