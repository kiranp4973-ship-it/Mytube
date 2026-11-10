package com.example.data.remote

import com.example.data.model.YouTubeOEmbedResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

data class ParsedYouTubeVideo(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val originalUrl: String
)

class YouTubeMetadataService(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(YouTubeOEmbedResponse::class.java)

    companion object {
        private val YOUTUBE_URL_PATTERNS = listOf(
            Pattern.compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?youtube\\.com\\/watch\\?.*v=([a-zA-Z0-9_-]{11})"),
            Pattern.compile("(?:https?:\\/\\/)?youtu\\.be\\/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?youtube\\.com\\/embed\\/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?youtube\\.com\\/shorts\\/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("^([a-zA-Z0-9_-]{11})$")
        )

        fun extractVideoId(url: String): String? {
            val cleanUrl = url.trim()
            for (pattern in YOUTUBE_URL_PATTERNS) {
                val matcher = pattern.matcher(cleanUrl)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            return null
        }

        fun getStandardThumbnailUrl(videoId: String): String {
            return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }

    suspend fun fetchMetadata(inputUrlOrId: String): Result<ParsedYouTubeVideo> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(inputUrlOrId)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL or Video ID"))

        val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        val fallbackThumbnail = getStandardThumbnailUrl(videoId)

        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=$canonicalUrl&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val oembed = adapter.fromJson(body)
                    val title = oembed?.title?.ifBlank { null } ?: "YouTube Video ($videoId)"
                    val channel = oembed?.authorName?.ifBlank { null } ?: "YouTube Creator"
                    val thumb = oembed?.thumbnailUrl?.ifBlank { null } ?: fallbackThumbnail

                    return@withContext Result.success(
                        ParsedYouTubeVideo(
                            videoId = videoId,
                            title = title,
                            channelTitle = channel,
                            thumbnailUrl = thumb,
                            originalUrl = canonicalUrl
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully below
        }

        // Graceful fallback if oEmbed is offline or rate limited
        Result.success(
            ParsedYouTubeVideo(
                videoId = videoId,
                title = "YouTube Video ($videoId)",
                channelTitle = "YouTube Creator",
                thumbnailUrl = fallbackThumbnail,
                originalUrl = canonicalUrl
            )
        )
    }
}
