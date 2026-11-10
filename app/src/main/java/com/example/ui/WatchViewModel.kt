package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.WatchDatabase
import com.example.data.model.WatchStatus
import com.example.data.model.WatchVideo
import com.example.data.remote.ParsedYouTubeVideo
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseConnectionTestResult
import com.example.data.repository.AuthRepository
import com.example.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    WATCH_LIST("Watch List"),
    PLAYLISTS("Playlists"),
    HISTORY("History")
}

data class WatchUiState(
    val currentTab: AppTab = AppTab.DASHBOARD,
    val allVideos: List<WatchVideo> = emptyList(),
    val filteredVideos: List<WatchVideo> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val selectedStatus: WatchStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val supabaseWatchTable: String = "watch_list",
    val supabaseUserTable: String = "user_manager",
    val isSupabaseConfigured: Boolean = false,
    val connectionTestResult: SupabaseConnectionTestResult? = null,
    val isTestingConnection: Boolean = false,
    val userId: String = "local_user",
    val userEmail: String = "",
    val isAuthenticated: Boolean = false,
    val userMessage: String? = null,
    // Active dialogs
    val showAddEditDialog: Boolean = false,
    val editingVideo: WatchVideo? = null,
    val showProgressDialog: Boolean = false,
    val activeProgressVideo: WatchVideo? = null,
    val showAuthDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val videoToDelete: WatchVideo? = null
) {
    val continueWatchingVideos: List<WatchVideo>
        get() = allVideos.filter {
            it.watchStatus == WatchStatus.WATCHING || (it.progressSeconds > 0 && it.watchStatus != WatchStatus.WATCHED)
        }.sortedByDescending { it.lastWatchedAt.takeIf { t -> t > 0L } ?: it.updatedAt }

    val nextToWatchVideos: List<WatchVideo>
        get() = allVideos.filter {
            it.watchStatus == WatchStatus.TO_WATCH
        }.sortedByDescending { it.createdAt }

    val completedVideos: List<WatchVideo>
        get() = allVideos.filter {
            it.watchStatus == WatchStatus.WATCHED
        }.sortedByDescending { it.lastWatchedAt.takeIf { t -> t > 0L } ?: it.updatedAt }

    val historyVideos: List<WatchVideo>
        get() = allVideos.filter { it.lastWatchedAt > 0L }
            .sortedByDescending { it.lastWatchedAt }

    val totalCount: Int get() = allVideos.size
    val watchingCount: Int get() = allVideos.count { it.watchStatus == WatchStatus.WATCHING }
    val toWatchCount: Int get() = allVideos.count { it.watchStatus == WatchStatus.TO_WATCH }
    val completedCount: Int get() = allVideos.count { it.watchStatus == WatchStatus.WATCHED }
    val totalSecondsWatched: Int get() = allVideos.sumOf { it.progressSeconds }
}

class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = UserPreferences(application)
    private val database = WatchDatabase.getInstance(application)
    private val supabaseApi = SupabaseApiService()
    private val watchRepository = WatchRepository(database.watchVideoDao(), preferences, supabaseApi)
    private val authRepository = AuthRepository(preferences, supabaseApi)

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences & auth
        viewModelScope.launch {
            combine(
                preferences.supabaseUrlFlow,
                preferences.supabaseAnonKeyFlow,
                preferences.watchTableFlow,
                preferences.userTableFlow
            ) { url, anonKey, watchTbl, userTbl ->
                _uiState.update {
                    it.copy(
                        supabaseUrl = url,
                        supabaseAnonKey = anonKey,
                        supabaseWatchTable = watchTbl,
                        supabaseUserTable = userTbl,
                        isSupabaseConfigured = url.isNotBlank() && anonKey.isNotBlank()
                    )
                }
            }.collectLatest { }
        }

        viewModelScope.launch {
            combine(
                preferences.userIdFlow,
                preferences.userEmailFlow,
                preferences.authTokenFlow
            ) { uid, email, token ->
                _uiState.update {
                    it.copy(
                        userId = uid,
                        userEmail = email,
                        isAuthenticated = token.isNotBlank()
                    )
                }
            }.collectLatest { }
        }

        // Initialize sample data if needed and observe room videos
        viewModelScope.launch {
            preferences.userIdFlow.collectLatest { uid ->
                watchRepository.checkAndSeedInitialData(uid)
                watchRepository.getVideos(uid).collectLatest { videos ->
                    _uiState.update { current ->
                        val distinctCats = (listOf("All") + videos.map { v -> v.category.ifBlank { "General" } }.distinct().sorted())
                        val filtered = filterVideos(videos, current.searchQuery, current.selectedCategory, current.selectedStatus)
                        current.copy(
                            allVideos = videos,
                            filteredVideos = filtered,
                            categories = distinctCats
                        )
                    }
                }
            }
        }
    }

    private fun filterVideos(
        list: List<WatchVideo>,
        query: String,
        category: String,
        status: WatchStatus?
    ): List<WatchVideo> {
        return list.filter { video ->
            val matchesQuery = query.isBlank() ||
                video.title.contains(query, ignoreCase = true) ||
                video.channelTitle.contains(query, ignoreCase = true) ||
                video.category.contains(query, ignoreCase = true) ||
                video.notes.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || video.category.equals(category, ignoreCase = true)
            val matchesStatus = status == null || video.watchStatus == status

            matchesQuery && matchesCategory && matchesStatus
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = filterVideos(current.allVideos, query, current.selectedCategory, current.selectedStatus)
            current.copy(searchQuery = query, filteredVideos = filtered)
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { current ->
            val filtered = filterVideos(current.allVideos, current.searchQuery, category, current.selectedStatus)
            current.copy(selectedCategory = category, filteredVideos = filtered)
        }
    }

    fun selectStatusFilter(status: WatchStatus?) {
        _uiState.update { current ->
            val filtered = filterVideos(current.allVideos, current.searchQuery, current.selectedCategory, status)
            current.copy(selectedStatus = status, filteredVideos = filtered)
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddEditDialog = true, editingVideo = null) }
    }

    fun openEditDialog(video: WatchVideo) {
        _uiState.update { it.copy(showAddEditDialog = true, editingVideo = video) }
    }

    fun closeAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingVideo = null) }
    }

    fun openProgressDialog(video: WatchVideo) {
        _uiState.update { it.copy(showProgressDialog = true, activeProgressVideo = video) }
    }

    fun closeProgressDialog() {
        _uiState.update { it.copy(showProgressDialog = false, activeProgressVideo = null) }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun closeSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false, connectionTestResult = null) }
    }

    fun openAuthDialog() {
        _uiState.update { it.copy(showAuthDialog = true) }
    }

    fun closeAuthDialog() {
        _uiState.update { it.copy(showAuthDialog = false) }
    }

    fun confirmDelete(video: WatchVideo?) {
        _uiState.update { it.copy(videoToDelete = video) }
    }

    suspend fun fetchYouTubeMetadata(url: String): Result<ParsedYouTubeVideo> {
        return watchRepository.parseYouTubeUrl(url)
    }

    fun saveVideo(video: WatchVideo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isEdit = _uiState.value.allVideos.any { it.id == video.id }
            val result = if (isEdit) {
                watchRepository.updateVideo(video)
            } else {
                watchRepository.saveVideo(video)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showAddEditDialog = false,
                    editingVideo = null,
                    userMessage = if (result.isSuccess) "Saved '${video.title}' successfully" else "Failed to save: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun updateStatus(videoId: String, status: WatchStatus) {
        viewModelScope.launch {
            watchRepository.updateStatus(videoId, status)
        }
    }

    fun updateProgress(videoId: String, seconds: Int, duration: Int) {
        viewModelScope.launch {
            watchRepository.updatePlaybackProgress(videoId, seconds, duration)
            closeProgressDialog()
        }
    }

    fun deleteConfirmedVideo() {
        val video = _uiState.value.videoToDelete ?: return
        viewModelScope.launch {
            watchRepository.deleteVideo(video.id)
            _uiState.update {
                it.copy(
                    videoToDelete = null,
                    userMessage = "Deleted '${video.title}'"
                )
            }
        }
    }

    fun syncWithSupabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Syncing with Supabase...") }
            val result = watchRepository.syncWithSupabase()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = if (result.isSuccess) "Synced ${result.getOrNull()} videos" else "Sync failed: ${result.exceptionOrNull()?.message}",
                    userMessage = if (result.isSuccess) "Sync completed successfully" else "Sync error: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun testSupabaseConnection(url: String, anonKey: String, watchTable: String, userTable: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true) }
            val result = supabaseApi.testConnection(url, anonKey, watchTable, userTable)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestResult = result
                )
            }
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String, watchTable: String, userTable: String) {
        viewModelScope.launch {
            preferences.saveSupabaseConfig(url, anonKey, watchTable, userTable)
            _uiState.update { it.copy(userMessage = "Supabase configuration saved!") }
        }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.signIn(email, pass)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                _uiState.update { s -> s.copy(showAuthDialog = false, userMessage = "Signed in as $email") }
                syncWithSupabase()
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.signUp(email, pass)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                _uiState.update { s -> s.copy(showAuthDialog = false, userMessage = "Account created! Signed in as $email") }
                syncWithSupabase()
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(userMessage = "Signed out") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
