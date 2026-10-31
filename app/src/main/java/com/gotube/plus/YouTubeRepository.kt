package com.gotube.plus

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class YouTubeSearchPage(
    val videos: List<VideoItem>,
    val nextPageToken: String?
)

data class YouTubeCategory(val id: String?, val name: String)

val YouTubeCategories = listOf(
    YouTubeCategory(null, "Semua"),
    YouTubeCategory("1", "Film"),
    YouTubeCategory("10", "Musik"),
    YouTubeCategory("17", "Olahraga"),
    YouTubeCategory("20", "Gaming"),
    YouTubeCategory("22", "Vlog"),
    YouTubeCategory("23", "Komedi"),
    YouTubeCategory("24", "Hiburan"),
    YouTubeCategory("25", "Berita"),
    YouTubeCategory("26", "Tutorial"),
    YouTubeCategory("27", "Edukasi"),
    YouTubeCategory("28", "Teknologi")
)

class YouTubeApiException(
    message: String,
    val isQuotaExceeded: Boolean
) : Exception(message)

class YouTubeRepository(
    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun search(
        query: String,
        categoryId: String? = null,
        pageToken: String? = null
    ): YouTubeSearchPage = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw YouTubeApiException("API key YouTube belum dikonfigurasi.", isQuotaExceeded = false)
        }

        val requestUri = Uri.parse(SEARCH_ENDPOINT).buildUpon()
            .appendQueryParameter("part", "snippet")
            .appendQueryParameter("type", "video")
            .appendQueryParameter("maxResults", PAGE_SIZE.toString())
            .appendQueryParameter("q", query)
            .appendQueryParameter("regionCode", "ID")
            .appendQueryParameter("relevanceLanguage", "id")
            .appendQueryParameter("key", apiKey)
            .apply {
                categoryId?.takeIf(String::isNotBlank)?.let {
                    appendQueryParameter("videoCategoryId", it)
                }
                pageToken?.takeIf(String::isNotBlank)?.let {
                    appendQueryParameter("pageToken", it)
                }
            }
            .build()

        val connection = URL(requestUri.toString()).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            val statusCode = connection.responseCode
            val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) throw toApiException(body, statusCode)
            parseSearchResponse(body)
        } catch (error: YouTubeApiException) {
            throw error
        } catch (error: Exception) {
            throw YouTubeApiException(
                error.message ?: "Tidak dapat terhubung ke YouTube.",
                isQuotaExceeded = false
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseSearchResponse(body: String): YouTubeSearchPage {
        val response = JSONObject(body)
        val items = response.optJSONArray("items")
        val videos = buildList {
            if (items == null) return@buildList
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val videoId = item.optJSONObject("id")?.optString("videoId")?.takeIf(String::isNotBlank)
                    ?: continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbnail = listOf("high", "medium", "default")
                    .firstNotNullOfOrNull { thumbnails?.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank) }
                    .orEmpty()

                add(
                    VideoItem(
                        id = "youtube:$videoId",
                        title = snippet.optString("title", "Video YouTube"),
                        creator = snippet.optString("channelTitle", "Channel YouTube"),
                        description = snippet.optString("description"),
                        streamUrl = null,
                        thumbnailUrl = thumbnail,
                        durationLabel = "YouTube",
                        youtubeId = videoId
                    )
                )
            }
        }
        return YouTubeSearchPage(videos, response.optString("nextPageToken").takeIf(String::isNotBlank))
    }

    private fun toApiException(body: String, statusCode: Int): YouTubeApiException {
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        val message = error?.optString("message")?.takeIf(String::isNotBlank)
            ?: "Permintaan YouTube gagal (HTTP $statusCode)."
        val reasons = error?.optJSONArray("errors")
        val reason = reasons?.optJSONObject(0)?.optString("reason").orEmpty()
        val quotaExceeded = reason in QUOTA_REASONS || statusCode == 429
        return YouTubeApiException(
            if (quotaExceeded) "Kuota pencarian YouTube hari ini habis. Coba lagi nanti." else message,
            isQuotaExceeded = quotaExceeded
        )
    }

    private companion object {
        const val SEARCH_ENDPOINT = "https://www.googleapis.com/youtube/v3/search"
        const val PAGE_SIZE = 20
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        val QUOTA_REASONS = setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded")
    }
}