package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class WatchStatus(val displayName: String, val value: String) {
    TO_WATCH("To Watch", "to_watch"),
    WATCHING("Watching", "watching"),
    WATCHED("Watched", "watched");

    companion object {
        fun fromValue(value: String?): WatchStatus {
            return when (value?.lowercase()?.trim()) {
                "watching", "in_progress" -> WATCHING
                "watched", "completed" -> WATCHED
                else -> TO_WATCH
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class WatchVideo(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "video_id") val videoId: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "channel_title") val channelTitle: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "category") val category: String = "General",
    @Json(name = "status") val status: String = WatchStatus.TO_WATCH.value,
    @Json(name = "progress_seconds") val progressSeconds: Int = 0,
    @Json(name = "duration_seconds") val durationSeconds: Int = 0,
    @Json(name = "last_watched_at") val lastWatchedAt: Long = 0L,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    val watchStatus: WatchStatus
        get() = WatchStatus.fromValue(status)

    val progressPercent: Float
        get() = if (durationSeconds > 0) {
            (progressSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
        } else if (watchStatus == WatchStatus.WATCHED) {
            1f
        } else if (watchStatus == WatchStatus.WATCHING) {
            0.5f
        } else 0f

    fun formatSeconds(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        val h = m / 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m % 60, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    val formattedProgress: String
        get() = formatSeconds(progressSeconds)

    val formattedDuration: String
        get() = if (durationSeconds > 0) formatSeconds(durationSeconds) else ""
}
