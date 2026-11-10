package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WatchDatabase
import com.example.data.local.WatchVideoEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchVideo
import com.example.data.remote.YouTubeMetadataService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WatchManagerUnitTest {

    private lateinit var context: Context
    private lateinit var db: WatchDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(
            context,
            WatchDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Watch Manager", appName)
    }

    @Test
    fun testYouTubeUrlExtractor() {
        val standardUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val shortUrl = "https://youtu.be/dQw4w9WgXcQ"
        val embedUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ"
        val shortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        val rawId = "dQw4w9WgXcQ"

        assertEquals("dQw4w9WgXcQ", YouTubeMetadataService.extractVideoId(standardUrl))
        assertEquals("dQw4w9WgXcQ", YouTubeMetadataService.extractVideoId(shortUrl))
        assertEquals("dQw4w9WgXcQ", YouTubeMetadataService.extractVideoId(embedUrl))
        assertEquals("dQw4w9WgXcQ", YouTubeMetadataService.extractVideoId(shortsUrl))
        assertEquals("dQw4w9WgXcQ", YouTubeMetadataService.extractVideoId(rawId))
    }

    @Test
    fun testWatchVideoModelProgress() {
        val video = WatchVideo(
            id = "test-1",
            userId = "user-1",
            videoId = "dQw4w9WgXcQ",
            title = "Test Video",
            progressSeconds = 120,
            durationSeconds = 240,
            status = WatchStatus.WATCHING.value
        )

        assertEquals(0.5f, video.progressPercent, 0.01f)
        assertEquals("02:00", video.formattedProgress)
        assertEquals("04:00", video.formattedDuration)
    }

    @Test
    fun testRoomDatabaseSaveUpdateDelete() = runBlocking {
        val dao = db.watchVideoDao()
        val userId = "test_user_1"

        val video = WatchVideo(
            id = "vid_1",
            userId = userId,
            videoId = "EngW7tLk6R8",
            videoUrl = "https://www.youtube.com/watch?v=EngW7tLk6R8",
            title = "Test Video",
            channelTitle = "Kurzgesagt",
            category = "Education",
            status = WatchStatus.TO_WATCH.value,
            progressSeconds = 0,
            durationSeconds = 600
        )

        // 1. Insert video
        dao.insertOrUpdate(WatchVideoEntity.fromDomain(video))

        // 2. Fetch all for user
        val userVideos = dao.getAllVideos(userId).first()
        assertEquals(1, userVideos.size)
        assertEquals("Test Video", userVideos[0].title)

        // 3. Update status to WATCHING and progress
        val updated = video.copy(
            status = WatchStatus.WATCHING.value,
            progressSeconds = 300
        )
        dao.insertOrUpdate(WatchVideoEntity.fromDomain(updated))

        val retrieved = dao.getVideoById(video.id)
        assertNotNull(retrieved)
        assertEquals(WatchStatus.WATCHING.value, retrieved!!.status)
        assertEquals(300, retrieved.progressSeconds)

        // 4. Delete video
        dao.deleteById(video.id)
        val afterDelete = dao.getAllVideos(userId).first()
        assertTrue(afterDelete.isEmpty())
    }
}
