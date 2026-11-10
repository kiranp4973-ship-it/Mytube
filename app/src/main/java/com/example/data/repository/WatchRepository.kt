package com.example.data.repository

import com.example.data.local.UserPreferences
import com.example.data.local.WatchVideoDao
import com.example.data.local.WatchVideoEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchVideo
import com.example.data.remote.ParsedYouTubeVideo
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseConnectionTestResult
import com.example.data.remote.YouTubeMetadataService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class WatchRepository(
    private val dao: WatchVideoDao,
    private val preferences: UserPreferences,
    private val supabaseApi: SupabaseApiService,
    private val youtubeService: YouTubeMetadataService = YouTubeMetadataService()
) {
    fun getVideos(userId: String): Flow<List<WatchVideo>> {
        return dao.getAllVideos(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getCategories(userId: String): Flow<List<String>> {
        return dao.getCategories(userId)
    }

    suspend fun parseYouTubeUrl(url: String): Result<ParsedYouTubeVideo> {
        return youtubeService.fetchMetadata(url)
    }

    suspend fun saveVideo(video: WatchVideo): Result<WatchVideo> {
        val currentUserId = preferences.userIdFlow.first()
        val videoId = if (video.id.isBlank()) UUID.randomUUID().toString() else video.id
        val now = System.currentTimeMillis()

        val normalized = video.copy(
            id = videoId,
            userId = if (video.userId.isNotBlank()) video.userId else currentUserId,
            updatedAt = now,
            createdAt = if (video.createdAt == 0L) now else video.createdAt
        )

        // 1. Save to Room database immediately for offline-first responsiveness
        dao.insertOrUpdate(WatchVideoEntity.fromDomain(normalized, isSynced = false))

        // 2. Sync to Supabase if configured
        val url = preferences.supabaseUrlFlow.first()
        val anonKey = preferences.supabaseAnonKeyFlow.first()
        val token = preferences.authTokenFlow.first()
        val watchTable = preferences.watchTableFlow.first()

        if (url.isNotBlank() && anonKey.isNotBlank()) {
            val remoteResult = supabaseApi.insertVideo(url, anonKey, token, watchTable, normalized)
            if (remoteResult.isSuccess) {
                dao.insertOrUpdate(WatchVideoEntity.fromDomain(normalized, isSynced = true))
            }
        }

        return Result.success(normalized)
    }

    suspend fun updateVideo(video: WatchVideo): Result<WatchVideo> {
        val now = System.currentTimeMillis()
        val updated = video.copy(updatedAt = now)

        dao.insertOrUpdate(WatchVideoEntity.fromDomain(updated, isSynced = false))

        val url = preferences.supabaseUrlFlow.first()
        val anonKey = preferences.supabaseAnonKeyFlow.first()
        val token = preferences.authTokenFlow.first()
        val watchTable = preferences.watchTableFlow.first()

        if (url.isNotBlank() && anonKey.isNotBlank()) {
            val remoteResult = supabaseApi.updateVideo(url, anonKey, token, watchTable, updated)
            if (remoteResult.isSuccess) {
                dao.insertOrUpdate(WatchVideoEntity.fromDomain(updated, isSynced = true))
            }
        }

        return Result.success(updated)
    }

    suspend fun updatePlaybackProgress(
        videoId: String,
        progressSeconds: Int,
        durationSeconds: Int = 0
    ): Result<Unit> {
        val existing = dao.getVideoById(videoId)?.toDomain() ?: return Result.failure(NoSuchElementException("Video not found"))
        val totalDuration = if (durationSeconds > 0) durationSeconds else existing.durationSeconds
        val newStatus = when {
            totalDuration > 0 && progressSeconds >= totalDuration -> WatchStatus.WATCHED.value
            progressSeconds > 0 -> WatchStatus.WATCHING.value
            else -> existing.status
        }

        val updated = existing.copy(
            progressSeconds = progressSeconds,
            durationSeconds = totalDuration,
            status = newStatus,
            lastWatchedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        updateVideo(updated)
        return Result.success(Unit)
    }

    suspend fun updateStatus(videoId: String, status: WatchStatus): Result<Unit> {
        val existing = dao.getVideoById(videoId)?.toDomain() ?: return Result.failure(NoSuchElementException("Video not found"))
        val progress = if (status == WatchStatus.WATCHED && existing.durationSeconds > 0) {
            existing.durationSeconds
        } else if (status == WatchStatus.TO_WATCH) {
            0
        } else {
            existing.progressSeconds
        }

        val updated = existing.copy(
            status = status.value,
            progressSeconds = progress,
            lastWatchedAt = if (status != WatchStatus.TO_WATCH) System.currentTimeMillis() else existing.lastWatchedAt,
            updatedAt = System.currentTimeMillis()
        )
        updateVideo(updated)
        return Result.success(Unit)
    }

    suspend fun deleteVideo(id: String): Result<Unit> {
        dao.deleteById(id)

        val url = preferences.supabaseUrlFlow.first()
        val anonKey = preferences.supabaseAnonKeyFlow.first()
        val token = preferences.authTokenFlow.first()
        val watchTable = preferences.watchTableFlow.first()

        if (url.isNotBlank() && anonKey.isNotBlank()) {
            supabaseApi.deleteVideo(url, anonKey, token, watchTable, id)
        }

        return Result.success(Unit)
    }

    suspend fun syncWithSupabase(): Result<Int> {
        val url = preferences.supabaseUrlFlow.first()
        val anonKey = preferences.supabaseAnonKeyFlow.first()
        val token = preferences.authTokenFlow.first()
        val watchTable = preferences.watchTableFlow.first()
        val userId = preferences.userIdFlow.first()

        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        val result = supabaseApi.fetchVideos(url, anonKey, token, watchTable, userId)
        return result.map { remoteVideos ->
            if (remoteVideos.isNotEmpty()) {
                val entities = remoteVideos.map { WatchVideoEntity.fromDomain(it, isSynced = true) }
                dao.insertAll(entities)
            }
            remoteVideos.size
        }
    }

    suspend fun testSupabaseConnection(): SupabaseConnectionTestResult {
        val url = preferences.supabaseUrlFlow.first()
        val anonKey = preferences.supabaseAnonKeyFlow.first()
        val watchTable = preferences.watchTableFlow.first()
        val userTable = preferences.userTableFlow.first()
        return supabaseApi.testConnection(url, anonKey, watchTable, userTable)
    }

    suspend fun checkAndSeedInitialData(userId: String) {
        val alreadyInitialized = preferences.isDemoInitializedFlow.first()
        if (alreadyInitialized) return

        val sampleVideos = listOf(
            WatchVideo(
                id = UUID.randomUUID().toString(),
                userId = userId,
                videoId = "1xipGff6AOY",
                videoUrl = "https://www.youtube.com/watch?v=1xipGff6AOY",
                title = "Android Jetpack Compose Full Crash Course 2026",
                channelTitle = "Android Developers",
                thumbnailUrl = "https://img.youtube.com/vi/1xipGff6AOY/hqdefault.jpg",
                category = "Tech",
                status = WatchStatus.WATCHING.value,
                progressSeconds = 480,
                durationSeconds = 1200,
                lastWatchedAt = System.currentTimeMillis() - 3600000L,
                notes = "Covers M3 themes, navigation, and state hoisting patterns."
            ),
            WatchVideo(
                id = UUID.randomUUID().toString(),
                userId = userId,
                videoId = "dQw4w9WgXcQ",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
                channelTitle = "Rick Astley",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                category = "Music",
                status = WatchStatus.WATCHED.value,
                progressSeconds = 212,
                durationSeconds = 212,
                lastWatchedAt = System.currentTimeMillis() - 86400000L,
                notes = "Classic masterpiece! 10/10."
            ),
            WatchVideo(
                id = UUID.randomUUID().toString(),
                userId = userId,
                videoId = "kJQP7kiw5Fk",
                videoUrl = "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
                title = "Luis Fonsi - Despacito ft. Daddy Yankee",
                channelTitle = "Luis Fonsi",
                thumbnailUrl = "https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
                category = "Music",
                status = WatchStatus.TO_WATCH.value,
                progressSeconds = 0,
                durationSeconds = 282,
                lastWatchedAt = 0L,
                notes = "Add to summer playlist."
            ),
            WatchVideo(
                id = UUID.randomUUID().toString(),
                userId = userId,
                videoId = "M576WGiDBdQ",
                videoUrl = "https://www.youtube.com/watch?v=M576WGiDBdQ",
                title = "Designing Intuitive Mobile Interfaces with Material You",
                channelTitle = "Google Design",
                thumbnailUrl = "https://img.youtube.com/vi/M576WGiDBdQ/hqdefault.jpg",
                category = "Design",
                status = WatchStatus.WATCHING.value,
                progressSeconds = 350,
                durationSeconds = 900,
                lastWatchedAt = System.currentTimeMillis() - 7200000L,
                notes = "Take notes on color contrast and elevation."
            ),
            WatchVideo(
                id = UUID.randomUUID().toString(),
                userId = userId,
                videoId = "EngW7tLk6R8",
                videoUrl = "https://www.youtube.com/watch?v=EngW7tLk6R8",
                title = "The Scale of the Universe Explained",
                channelTitle = "Kurzgesagt – In a Nutshell",
                thumbnailUrl = "https://img.youtube.com/vi/EngW7tLk6R8/hqdefault.jpg",
                category = "Education",
                status = WatchStatus.TO_WATCH.value,
                progressSeconds = 0,
                durationSeconds = 640,
                lastWatchedAt = 0L,
                notes = "Mind-blowing visuals and physics summary."
            )
        )

        dao.insertAll(sampleVideos.map { WatchVideoEntity.fromDomain(it, isSynced = false) })
        preferences.setDemoInitialized(true)
    }
}
