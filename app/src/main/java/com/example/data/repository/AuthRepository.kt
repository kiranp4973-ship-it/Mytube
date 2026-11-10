package com.example.data.repository

import com.example.data.local.UserPreferences
import com.example.data.model.SupabaseSession
import com.example.data.model.SupabaseUserProfile
import com.example.data.remote.SupabaseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed interface AuthState {
    data object Checking : AuthState
    data class Authenticated(val userId: String, val email: String, val token: String) : AuthState
    data class Guest(val localUserId: String) : AuthState
}

class AuthRepository(
    private val preferences: UserPreferences,
    private val apiService: SupabaseApiService
) {
    val userIdFlow: Flow<String> = preferences.userIdFlow
    val userEmailFlow: Flow<String> = preferences.userEmailFlow
    val authTokenFlow: Flow<String> = preferences.authTokenFlow

    suspend fun getSupabaseUrl(): String = preferences.supabaseUrlFlow.first()
    suspend fun getSupabaseAnonKey(): String = preferences.supabaseAnonKeyFlow.first()
    suspend fun getUserTable(): String = preferences.userTableFlow.first()

    suspend fun signIn(email: String, pass: String): Result<SupabaseSession> {
        val url = getSupabaseUrl()
        val anonKey = getSupabaseAnonKey()
        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(IllegalStateException("Supabase URL and Anon Key are not configured"))
        }

        val result = apiService.signIn(url, anonKey, email, pass)
        result.onSuccess { session ->
            val userId = session.user?.id.orEmpty()
            val userEmail = session.user?.email ?: email
            preferences.saveAuthSession(session.accessToken, userId, userEmail)

            // Try syncing user record to User Manager table
            val userTable = getUserTable()
            apiService.syncUserRecord(
                url,
                anonKey,
                session.accessToken,
                userTable,
                SupabaseUserProfile(
                    id = userId,
                    email = userEmail,
                    username = userEmail.substringBefore('@'),
                    lastSignInAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString()
                )
            )
        }
        return result
    }

    suspend fun signUp(email: String, pass: String): Result<SupabaseSession> {
        val url = getSupabaseUrl()
        val anonKey = getSupabaseAnonKey()
        if (url.isBlank() || anonKey.isBlank()) {
            return Result.failure(IllegalStateException("Supabase URL and Anon Key are not configured"))
        }

        val result = apiService.signUp(url, anonKey, email, pass)
        result.onSuccess { session ->
            if (session.accessToken.isNotBlank()) {
                val userId = session.user?.id.orEmpty()
                val userEmail = session.user?.email ?: email
                preferences.saveAuthSession(session.accessToken, userId, userEmail)

                val userTable = getUserTable()
                apiService.syncUserRecord(
                    url,
                    anonKey,
                    session.accessToken,
                    userTable,
                    SupabaseUserProfile(
                        id = userId,
                        email = userEmail,
                        username = userEmail.substringBefore('@'),
                        lastSignInAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        return result
    }

    suspend fun signOut(): Result<Unit> {
        val url = getSupabaseUrl()
        val anonKey = getSupabaseAnonKey()
        val token = preferences.authTokenFlow.first()

        if (url.isNotBlank() && anonKey.isNotBlank() && token.isNotBlank()) {
            apiService.signOut(url, anonKey, token)
        }
        preferences.clearAuthSession()
        return Result.success(Unit)
    }
}
