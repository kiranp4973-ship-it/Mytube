package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "id") val id: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSession(
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Long = 3600,
    @Json(name = "refresh_token") val refreshToken: String = "",
    @Json(name = "user") val user: SupabaseUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserProfile(
    @Json(name = "id") val id: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "username") val username: String? = null,
    @Json(name = "last_sign_in_at") val lastSignInAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_url") val authorUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)
