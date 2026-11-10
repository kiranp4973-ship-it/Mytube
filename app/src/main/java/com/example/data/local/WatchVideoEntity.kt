package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.WatchVideo

@Entity(tableName = "watch_videos")
data class WatchVideoEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val videoUrl: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val category: String,
    val status: String,
    val progressSeconds: Int,
    val durationSeconds: Int,
    val lastWatchedAt: Long,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSyncedWithSupabase: Boolean = true
) {
    fun toDomain(): WatchVideo = WatchVideo(
        id = id,
        userId = userId,
        videoId = videoId,
        videoUrl = videoUrl,
        title = title,
        channelTitle = channelTitle,
        thumbnailUrl = thumbnailUrl,
        category = category,
        status = status,
        progressSeconds = progressSeconds,
        durationSeconds = durationSeconds,
        lastWatchedAt = lastWatchedAt,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(video: WatchVideo, isSynced: Boolean = true): WatchVideoEntity = WatchVideoEntity(
            id = video.id,
            userId = video.userId,
            videoId = video.videoId,
            videoUrl = video.videoUrl,
            title = video.title,
            channelTitle = video.channelTitle,
            thumbnailUrl = video.thumbnailUrl,
            category = video.category,
            status = video.status,
            progressSeconds = video.progressSeconds,
            durationSeconds = video.durationSeconds,
            lastWatchedAt = video.lastWatchedAt,
            notes = video.notes,
            createdAt = video.createdAt,
            updatedAt = video.updatedAt,
            isSyncedWithSupabase = isSynced
        )
    }
}
