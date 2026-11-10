package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
        val KEY_SUPABASE_WATCH_TABLE = stringPreferencesKey("supabase_watch_table")
        val KEY_SUPABASE_USER_TABLE = stringPreferencesKey("supabase_user_table")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_DEMO_INITIALIZED = booleanPreferencesKey("demo_initialized")
    }

    val supabaseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_URL] ?: ""
    }

    val supabaseAnonKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_ANON_KEY] ?: ""
    }

    val watchTableFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_WATCH_TABLE] ?: "watch_list"
    }

    val userTableFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_USER_TABLE] ?: "user_manager"
    }

    val authTokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN] ?: ""
    }

    val userIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID] ?: "local_user"
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL] ?: ""
    }

    val isDarkThemeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs.contains(KEY_DARK_THEME)) prefs[KEY_DARK_THEME] else null
    }

    val isDemoInitializedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEMO_INITIALIZED] ?: false
    }

    suspend fun saveSupabaseConfig(url: String, anonKey: String, watchTable: String = "watch_list", userTable: String = "user_manager") {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_URL] = url.trim().trimEnd('/')
            prefs[KEY_SUPABASE_ANON_KEY] = anonKey.trim()
            prefs[KEY_SUPABASE_WATCH_TABLE] = watchTable.trim()
            prefs[KEY_SUPABASE_USER_TABLE] = userTable.trim()
        }
    }

    suspend fun saveAuthSession(token: String, userId: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
        }
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = ""
            prefs[KEY_USER_ID] = "local_user"
            prefs[KEY_USER_EMAIL] = ""
        }
    }

    suspend fun setDarkTheme(enabled: Boolean?) {
        context.dataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(KEY_DARK_THEME)
            } else {
                prefs[KEY_DARK_THEME] = enabled
            }
        }
    }

    suspend fun setDemoInitialized(initialized: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO_INITIALIZED] = initialized
        }
    }
}
