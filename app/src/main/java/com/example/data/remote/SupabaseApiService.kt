package com.example.data.remote

import com.example.data.model.SupabaseAuthRequest
import com.example.data.model.SupabaseSession
import com.example.data.model.SupabaseUserProfile
import com.example.data.model.WatchVideo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SupabaseConnectionTestResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val watchTableStatus: String,
    val userTableStatus: String,
    val message: String
)

class SupabaseApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val sessionAdapter = moshi.adapter(SupabaseSession::class.java)
    private val authReqAdapter = moshi.adapter(SupabaseAuthRequest::class.java)
    private val videoAdapter = moshi.adapter(WatchVideo::class.java)
    private val videoListAdapter = moshi.adapter<List<WatchVideo>>(
        Types.newParameterizedType(List::class.java, WatchVideo::class.java)
    )
    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    private fun normalizeUrl(url: String): String = url.trim().trimEnd('/')

    private fun getAuthHeader(token: String, anonKey: String): String {
        return if (token.isNotBlank()) "Bearer $token" else "Bearer $anonKey"
    }

    suspend fun testConnection(
        rawUrl: String,
        anonKey: String,
        watchTable: String,
        userTable: String
    ): SupabaseConnectionTestResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            return@withContext SupabaseConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                watchTableStatus = "Not configured",
                userTableStatus = "Not configured",
                message = "Supabase URL and Anon Key are required."
            )
        }

        val startTime = System.currentTimeMillis()
        var statusCode = 0
        var watchStatus = "Checking..."
        var userStatus = "Checking..."

        try {
            // Ping root REST OpenAPI schema
            val rootReq = Request.Builder()
                .url("$baseUrl/rest/v1/")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()

            val rootRes = client.newCall(rootReq).execute()
            statusCode = rootRes.code
            val latency = System.currentTimeMillis() - startTime

            // Probe watch table
            val watchReq = Request.Builder()
                .url("$baseUrl/rest/v1/$watchTable?select=count&limit=0")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()
            val watchRes = client.newCall(watchReq).execute()
            watchStatus = if (watchRes.isSuccessful) {
                "Found & Active (HTTP ${watchRes.code})"
            } else if (watchRes.code == 404) {
                "Table '$watchTable' not found (check spelling or case)"
            } else {
                "HTTP ${watchRes.code} (RLS may require user login)"
            }

            // Probe user table
            val userReq = Request.Builder()
                .url("$baseUrl/rest/v1/$userTable?select=count&limit=0")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()
            val userRes = client.newCall(userReq).execute()
            userStatus = if (userRes.isSuccessful) {
                "Found & Active (HTTP ${userRes.code})"
            } else if (userRes.code == 404) {
                "Table '$userTable' not found"
            } else {
                "HTTP ${userRes.code} (RLS protected)"
            }

            return@withContext SupabaseConnectionTestResult(
                isSuccess = statusCode in 200..299,
                statusCode = statusCode,
                latencyMs = latency,
                watchTableStatus = watchStatus,
                userTableStatus = userStatus,
                message = if (statusCode in 200..299) "Successfully connected to Supabase!" else "Supabase responded with code $statusCode"
            )
        } catch (e: Exception) {
            return@withContext SupabaseConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = System.currentTimeMillis() - startTime,
                watchTableStatus = "Error",
                userTableStatus = "Error",
                message = "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun signIn(rawUrl: String, anonKey: String, email: String, pass: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val bodyJson = authReqAdapter.toJson(SupabaseAuthRequest(email = email, password = pass))
            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=password")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val session = sessionAdapter.fromJson(responseBody)
                if (session != null && session.accessToken.isNotBlank()) {
                    Result.success(session)
                } else {
                    Result.failure(IOException("Invalid auth response: $responseBody"))
                }
            } else {
                Result.failure(IOException("Sign in failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(rawUrl: String, anonKey: String, email: String, pass: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val bodyJson = authReqAdapter.toJson(SupabaseAuthRequest(email = email, password = pass))
            val request = Request.Builder()
                .url("$baseUrl/auth/v1/signup")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val session = sessionAdapter.fromJson(responseBody)
                if (session != null) {
                    Result.success(session)
                } else {
                    Result.failure(IOException("Empty signup session: $responseBody"))
                }
            } else {
                Result.failure(IOException("Sign up failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(rawUrl: String, anonKey: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val request = Request.Builder()
                .url("$baseUrl/auth/v1/logout")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $token")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchVideos(
        rawUrl: String,
        anonKey: String,
        token: String,
        tableName: String,
        userId: String
    ): Result<List<WatchVideo>> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val queryParam = if (userId.isNotBlank() && userId != "local_user") {
                "?user_id=eq.$userId&order=updated_at.desc"
            } else {
                "?order=updated_at.desc"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/$tableName$queryParam")
                .header("apikey", anonKey)
                .header("Authorization", getAuthHeader(token, anonKey))
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val list = videoListAdapter.fromJson(responseBody) ?: emptyList()
                Result.success(list)
            } else {
                Result.failure(IOException("Fetch videos failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertVideo(
        rawUrl: String,
        anonKey: String,
        token: String,
        tableName: String,
        video: WatchVideo
    ): Result<WatchVideo> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val json = videoAdapter.toJson(video)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/$tableName")
                .header("apikey", anonKey)
                .header("Authorization", getAuthHeader(token, anonKey))
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val list = videoListAdapter.fromJson(responseBody)
                val returnedVideo = list?.firstOrNull() ?: video
                Result.success(returnedVideo)
            } else {
                Result.failure(IOException("Insert video failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVideo(
        rawUrl: String,
        anonKey: String,
        token: String,
        tableName: String,
        video: WatchVideo
    ): Result<WatchVideo> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val json = videoAdapter.toJson(video)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/$tableName?id=eq.${video.id}")
                .header("apikey", anonKey)
                .header("Authorization", getAuthHeader(token, anonKey))
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .patch(json.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val list = videoListAdapter.fromJson(responseBody)
                val returnedVideo = list?.firstOrNull() ?: video
                Result.success(returnedVideo)
            } else {
                Result.failure(IOException("Update video failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(
        rawUrl: String,
        anonKey: String,
        token: String,
        tableName: String,
        id: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/$tableName?id=eq.$id")
                .header("apikey", anonKey)
                .header("Authorization", getAuthHeader(token, anonKey))
                .delete()
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserRecord(
        rawUrl: String,
        anonKey: String,
        token: String,
        userTable: String,
        profile: SupabaseUserProfile
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        try {
            val json = moshi.adapter(SupabaseUserProfile::class.java).toJson(profile)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/$userTable")
                .header("apikey", anonKey)
                .header("Authorization", getAuthHeader(token, anonKey))
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
