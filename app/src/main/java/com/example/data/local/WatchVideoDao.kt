package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchVideoDao {
    @Query("SELECT * FROM watch_videos WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllVideos(userId: String): Flow<List<WatchVideoEntity>>

    @Query("SELECT * FROM watch_videos ORDER BY updatedAt DESC")
    fun getAllVideosFlow(): Flow<List<WatchVideoEntity>>

    @Query("SELECT * FROM watch_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): WatchVideoEntity?

    @Query("SELECT * FROM watch_videos WHERE userId = :userId AND status = :status ORDER BY updatedAt DESC")
    fun getVideosByStatus(userId: String, status: String): Flow<List<WatchVideoEntity>>

    @Query("SELECT * FROM watch_videos WHERE userId = :userId AND category = :category ORDER BY updatedAt DESC")
    fun getVideosByCategory(userId: String, category: String): Flow<List<WatchVideoEntity>>

    @Query("SELECT DISTINCT category FROM watch_videos WHERE userId = :userId AND category != ''")
    fun getCategories(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(video: WatchVideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<WatchVideoEntity>)

    @Update
    suspend fun update(video: WatchVideoEntity)

    @Query("DELETE FROM watch_videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM watch_videos WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM watch_videos")
    suspend fun clearAll()
}
