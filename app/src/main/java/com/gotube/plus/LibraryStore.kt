package com.gotube.plus

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "gotube_library")

class LibraryStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.libraryDataStore
    private val preferences = appContext.getSharedPreferences("gotube_plus", Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()

    val favoriteVideos: Flow<List<VideoItem>> = dataStore.data
        .onStart { migrateLegacyLibrary() }
        .map { decodeVideos(it[FAVORITES_KEY]) }

    val historyVideos: Flow<List<VideoItem>> = dataStore.data
        .onStart { migrateLegacyLibrary() }
        .map { decodeVideos(it[HISTORY_KEY]) }

    suspend fun toggleFavorite(video: VideoItem) {
        migrateLegacyLibrary()
        dataStore.edit { values ->
            val current = decodeVideos(values[FAVORITES_KEY])
            val updated = if (current.any { it.id == video.id }) {
                current.filterNot { it.id == video.id }
            } else {
                listOf(video) + current
            }
            values[FAVORITES_KEY] = encodeVideos(updated)
        }
    }

    suspend fun addToHistory(video: VideoItem) {
        migrateLegacyLibrary()
        dataStore.edit { values ->
            val updated = (listOf(video) + decodeVideos(values[HISTORY_KEY]).filterNot { it.id == video.id })
                .take(MAX_HISTORY)
            values[HISTORY_KEY] = encodeVideos(updated)
        }
    }

    suspend fun clearHistory() {
        migrateLegacyLibrary()
        dataStore.edit { it[HISTORY_KEY] = "[]" }
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

    private suspend fun migrateLegacyLibrary() = migrationMutex.withLock {
        val current = dataStore.data.first()
        if (current[MIGRATED_KEY] == true) return@withLock

        val oldFavoriteIds = preferences.getStringSet(LEGACY_FAVORITES, emptySet()).orEmpty()
        val oldHistoryIds = preferences.getString(LEGACY_HISTORY, "")
            .orEmpty()
            .split('|')
            .filter(String::isNotBlank)
        dataStore.edit { values ->
            if (!values.contains(FAVORITES_KEY)) {
                values[FAVORITES_KEY] = encodeVideos(oldFavoriteIds.mapNotNull(VideoCatalog::find))
            }
            if (!values.contains(HISTORY_KEY)) {
                values[HISTORY_KEY] = encodeVideos(oldHistoryIds.mapNotNull(VideoCatalog::find))
            }
            values[MIGRATED_KEY] = true
        }
    }

    private fun encodeVideos(videos: List<VideoItem>): String = JSONArray().apply {
        videos.forEach { put(it.toJson()) }
    }.toString()

    private fun decodeVideos(raw: String?): List<VideoItem> = runCatching {
        val json = JSONArray(raw ?: "[]")
        List(json.length()) { index -> VideoItem.fromJson(json.getJSONObject(index)) }
    }.getOrDefault(emptyList())

    private companion object {
        val FAVORITES_KEY = stringPreferencesKey("favorite_videos")
        val HISTORY_KEY = stringPreferencesKey("history_videos")
        val MIGRATED_KEY = booleanPreferencesKey("legacy_library_migrated")
        const val LEGACY_FAVORITES = "favorites"
        const val LEGACY_HISTORY = "history"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_PIP = "picture_in_picture"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_POSITION = "position"
        const val KEY_WAS_PLAYING = "was_playing"
        const val MAX_HISTORY = 40
    }
}