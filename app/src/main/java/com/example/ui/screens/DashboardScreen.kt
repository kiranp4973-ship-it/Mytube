package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WatchStatus
import com.example.data.model.WatchVideo
import com.example.ui.AppTab
import com.example.ui.WatchViewModel
import com.example.ui.components.AddEditVideoDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.PlaybackProgressDialog
import com.example.ui.components.SupabaseSettingsDialog
import com.example.ui.components.VideoCard
import com.example.ui.theme.StatusToWatch
import com.example.ui.theme.StatusWatched
import com.example.ui.theme.StatusWatching
import com.example.ui.theme.YouTubeRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: WatchViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBarSection(
                uiState = uiState,
                onSyncClick = { viewModel.syncWithSupabase() },
                onAuthClick = { viewModel.openAuthDialog() },
                onSettingsClick = { viewModel.openSettingsDialog() }
            )
        },
        bottomBar = {
            BottomNavigationSection(
                currentTab = uiState.currentTab,
                onSelectTab = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = YouTubeRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("add_video_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add YouTube Video"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                AppTab.DASHBOARD -> DashboardHomeTab(
                    uiState = uiState,
                    onVideoProgress = { viewModel.openProgressDialog(it) },
                    onVideoEdit = { viewModel.openEditDialog(it) },
                    onVideoDelete = { viewModel.confirmDelete(it) },
                    onStatusChange = { id, status -> viewModel.updateStatus(id, status) },
                    onNavigateToWatchList = { viewModel.selectTab(AppTab.WATCH_LIST) }
                )
                AppTab.WATCH_LIST -> WatchListTab(
                    uiState = uiState,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onStatusFilterSelect = { viewModel.selectStatusFilter(it) },
                    onVideoProgress = { viewModel.openProgressDialog(it) },
                    onVideoEdit = { viewModel.openEditDialog(it) },
                    onVideoDelete = { viewModel.confirmDelete(it) },
                    onStatusChange = { id, status -> viewModel.updateStatus(id, status) }
                )
                AppTab.PLAYLISTS -> PlaylistsTab(
                    uiState = uiState,
                    onVideoProgress = { viewModel.openProgressDialog(it) },
                    onVideoEdit = { viewModel.openEditDialog(it) },
                    onVideoDelete = { viewModel.confirmDelete(it) },
                    onStatusChange = { id, status -> viewModel.updateStatus(id, status) },
                    onSelectCategory = {
                        viewModel.selectCategory(it)
                        viewModel.selectTab(AppTab.WATCH_LIST)
                    }
                )
                AppTab.HISTORY -> HistoryTab(
                    uiState = uiState,
                    onVideoProgress = { viewModel.openProgressDialog(it) },
                    onVideoEdit = { viewModel.openEditDialog(it) },
                    onVideoDelete = { viewModel.confirmDelete(it) },
                    onStatusChange = { id, status -> viewModel.updateStatus(id, status) }
                )
            }
        }
    }

    // Add / Edit Video Dialog
    if (uiState.showAddEditDialog) {
        AddEditVideoDialog(
            existingVideo = uiState.editingVideo,
            availableCategories = uiState.categories,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { video -> viewModel.saveVideo(video) },
            onFetchMetadata = { url -> viewModel.fetchYouTubeMetadata(url) }
        )
    }

    // Playback Progress Scrubber Dialog
    if (uiState.showProgressDialog && uiState.activeProgressVideo != null) {
        PlaybackProgressDialog(
            video = uiState.activeProgressVideo!!,
            onDismiss = { viewModel.closeProgressDialog() },
            onSaveProgress = { sec, duration ->
                viewModel.updateProgress(uiState.activeProgressVideo!!.id, sec, duration)
            }
        )
    }

    // Supabase Settings Dialog
    if (uiState.showSettingsDialog) {
        SupabaseSettingsDialog(
            currentUrl = uiState.supabaseUrl,
            currentAnonKey = uiState.supabaseAnonKey,
            currentWatchTable = uiState.supabaseWatchTable,
            currentUserTable = uiState.supabaseUserTable,
            isTesting = uiState.isTestingConnection,
            testResult = uiState.connectionTestResult,
            onDismiss = { viewModel.closeSettingsDialog() },
            onTestConnection = { url, key, wTbl, uTbl ->
                viewModel.testSupabaseConnection(url, key, wTbl, uTbl)
            },
            onSaveConfig = { url, key, wTbl, uTbl ->
                viewModel.saveSupabaseConfig(url, key, wTbl, uTbl)
            },
            onTriggerSync = { viewModel.syncWithSupabase() }
        )
    }

    // Supabase Auth Dialog
    if (uiState.showAuthDialog) {
        AuthDialog(
            isAuthenticated = uiState.isAuthenticated,
            currentUserEmail = uiState.userEmail,
            isLoading = uiState.isLoading,
            onDismiss = { viewModel.closeAuthDialog() },
            onSignIn = { email, pass, onSuccess, onError ->
                viewModel.signIn(email, pass, onSuccess, onError)
            },
            onSignUp = { email, pass, onSuccess, onError ->
                viewModel.signUp(email, pass, onSuccess, onError)
            },
            onSignOut = { viewModel.signOut() }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmDelete(null) },
            title = { Text("Delete Video?") },
            text = { Text("Are you sure you want to remove '${uiState.videoToDelete?.title}' from your watch list?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfirmedVideo() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.confirmDelete(null) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TopAppBarSection(
    uiState: com.example.ui.WatchUiState,
    onSyncClick: () -> Unit,
    onAuthClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
                    bottom = 10.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = YouTubeRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Watch Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personal YouTube Tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync icon
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier.testTag("sync_btn")
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync with Supabase",
                                tint = if (uiState.isSupabaseConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Account icon
                    IconButton(
                        onClick = onAuthClick,
                        modifier = Modifier.testTag("auth_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Supabase Account",
                            tint = if (uiState.isAuthenticated) StatusWatched else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings icon
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Supabase Settings"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supabase Status Bar Indicator
            Surface(
                color = when {
                    uiState.isAuthenticated -> StatusWatched.copy(alpha = 0.12f)
                    uiState.isSupabaseConfigured -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSettingsClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isSupabaseConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (uiState.isAuthenticated) StatusWatched else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                uiState.isAuthenticated -> "Supabase Connected: ${uiState.userEmail}"
                                uiState.isSupabaseConfigured -> "Supabase Configured (Guest / Anon Mode)"
                                else -> "Local Offline Mode (Tap to Connect Supabase)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "RLS Scoped",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationSection(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.DASHBOARD,
            onClick = { onSelectTab(AppTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard") },
            modifier = Modifier.testTag("tab_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.WATCH_LIST,
            onClick = { onSelectTab(AppTab.WATCH_LIST) },
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
            label = { Text("Watch List") },
            modifier = Modifier.testTag("tab_watchlist")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.PLAYLISTS,
            onClick = { onSelectTab(AppTab.PLAYLISTS) },
            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
            label = { Text("Playlists") },
            modifier = Modifier.testTag("tab_playlists")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.HISTORY,
            onClick = { onSelectTab(AppTab.HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
            modifier = Modifier.testTag("tab_history")
        )
    }
}

@Composable
private fun DashboardHomeTab(
    uiState: com.example.ui.WatchUiState,
    onVideoProgress: (WatchVideo) -> Unit,
    onVideoEdit: (WatchVideo) -> Unit,
    onVideoDelete: (WatchVideo) -> Unit,
    onStatusChange: (String, WatchStatus) -> Unit,
    onNavigateToWatchList: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Quick Stats row
        item {
            QuickStatsCards(uiState)
        }

        // Section: Continue Watching
        item {
            SectionHeader(
                title = "Continue Watching",
                badgeCount = uiState.continueWatchingVideos.size,
                actionText = "See All",
                onActionClick = onNavigateToWatchList
            )
        }

        if (uiState.continueWatchingVideos.isEmpty()) {
            item {
                EmptySectionPlaceholder(
                    message = "No videos in progress. Pick a video to start watching!",
                    icon = Icons.Default.PlayArrow
                )
            }
        } else {
            items(uiState.continueWatchingVideos, key = { "cw_" + it.id }) { video ->
                VideoCard(
                    video = video,
                    onProgressClick = { onVideoProgress(video) },
                    onEditClick = { onVideoEdit(video) },
                    onDeleteClick = { onVideoDelete(video) },
                    onStatusChange = { onStatusChange(video.id, it) }
                )
            }
        }

        // Section: Next to Watch
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Next to Watch",
                badgeCount = uiState.nextToWatchVideos.size,
                actionText = "Full List",
                onActionClick = onNavigateToWatchList
            )
        }

        if (uiState.nextToWatchVideos.isEmpty()) {
            item {
                EmptySectionPlaceholder(
                    message = "Your watch queue is clear. Tap '+' to add videos!",
                    icon = Icons.Default.VideoLibrary
                )
            }
        } else {
            items(uiState.nextToWatchVideos.take(4), key = { "nw_" + it.id }) { video ->
                VideoCard(
                    video = video,
                    onProgressClick = { onVideoProgress(video) },
                    onEditClick = { onVideoEdit(video) },
                    onDeleteClick = { onVideoDelete(video) },
                    onStatusChange = { onStatusChange(video.id, it) }
                )
            }
        }

        // Section: Completed
        if (uiState.completedVideos.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Completed Videos",
                    badgeCount = uiState.completedVideos.size
                )
            }
            items(uiState.completedVideos.take(3), key = { "comp_" + it.id }) { video ->
                VideoCard(
                    video = video,
                    onProgressClick = { onVideoProgress(video) },
                    onEditClick = { onVideoEdit(video) },
                    onDeleteClick = { onVideoDelete(video) },
                    onStatusChange = { onStatusChange(video.id, it) }
                )
            }
        }
    }
}

@Composable
private fun WatchListTab(
    uiState: com.example.ui.WatchUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onStatusFilterSelect: (WatchStatus?) -> Unit,
    onVideoProgress: (WatchVideo) -> Unit,
    onVideoEdit: (WatchVideo) -> Unit,
    onVideoDelete: (WatchVideo) -> Unit,
    onStatusChange: (String, WatchStatus) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by title, channel, playlist, or notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                singleLine = true
            )
        }

        // Category filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categories.forEach { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("filter_cat_$cat")
                    )
                }
            }
        }

        // Status filter pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatus == null,
                    onClick = { onStatusFilterSelect(null) },
                    label = { Text("All (${uiState.totalCount})") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.selectedStatus == WatchStatus.TO_WATCH,
                    onClick = { onStatusFilterSelect(WatchStatus.TO_WATCH) },
                    label = { Text("To Watch (${uiState.toWatchCount})") },
                    modifier = Modifier.weight(1.2f)
                )
                FilterChip(
                    selected = uiState.selectedStatus == WatchStatus.WATCHING,
                    onClick = { onStatusFilterSelect(WatchStatus.WATCHING) },
                    label = { Text("Watching (${uiState.watchingCount})") },
                    modifier = Modifier.weight(1.2f)
                )
                FilterChip(
                    selected = uiState.selectedStatus == WatchStatus.WATCHED,
                    onClick = { onStatusFilterSelect(WatchStatus.WATCHED) },
                    label = { Text("Done (${uiState.completedCount})") },
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        if (uiState.filteredVideos.isEmpty()) {
            item {
                EmptySectionPlaceholder(
                    message = if (uiState.searchQuery.isNotBlank()) "No videos matching '${uiState.searchQuery}'" else "No videos found in this filter",
                    icon = Icons.Default.FilterList
                )
            }
        } else {
            items(uiState.filteredVideos, key = { "wl_" + it.id }) { video ->
                VideoCard(
                    video = video,
                    onProgressClick = { onVideoProgress(video) },
                    onEditClick = { onVideoEdit(video) },
                    onDeleteClick = { onVideoDelete(video) },
                    onStatusChange = { onStatusChange(video.id, it) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    uiState: com.example.ui.WatchUiState,
    onVideoProgress: (WatchVideo) -> Unit,
    onVideoEdit: (WatchVideo) -> Unit,
    onVideoDelete: (WatchVideo) -> Unit,
    onStatusChange: (String, WatchStatus) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val grouped = remember(uiState.allVideos) {
        uiState.allVideos.groupBy { it.category.ifBlank { "General" } }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "My Playlists & Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${grouped.keys.size} playlists organized across your watch list",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (grouped.isEmpty()) {
            item {
                EmptySectionPlaceholder(
                    message = "No playlists yet. Add videos to create playlists!",
                    icon = Icons.Default.PlaylistPlay
                )
            }
        } else {
            grouped.forEach { (categoryName, videos) ->
                item(key = "cat_header_$categoryName") {
                    val completed = videos.count { it.watchStatus == WatchStatus.WATCHED }
                    val progressRatio = if (videos.isNotEmpty()) completed.toFloat() / videos.size.toFloat() else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(categoryName) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = YouTubeRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$completed / ${videos.size} watched",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = StatusWatched,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                items(videos.take(3), key = { "pl_${categoryName}_${it.id}" }) { video ->
                    VideoCard(
                        video = video,
                        onProgressClick = { onVideoProgress(video) },
                        onEditClick = { onVideoEdit(video) },
                        onDeleteClick = { onVideoDelete(video) },
                        onStatusChange = { onStatusChange(video.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    uiState: com.example.ui.WatchUiState,
    onVideoProgress: (WatchVideo) -> Unit,
    onVideoEdit: (WatchVideo) -> Unit,
    onVideoDelete: (WatchVideo) -> Unit,
    onStatusChange: (String, WatchStatus) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Watch History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Recently watched and updated positions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.historyVideos.isEmpty()) {
            item {
                EmptySectionPlaceholder(
                    message = "No watch activity recorded yet. Start watching to build your history!",
                    icon = Icons.Default.History
                )
            }
        } else {
            items(uiState.historyVideos, key = { "hist_" + it.id }) { video ->
                Column {
                    val dateLabel = if (video.lastWatchedAt > 0) dateFormat.format(Date(video.lastWatchedAt)) else "Recently saved"
                    Text(
                        text = "Last watched: $dateLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    VideoCard(
                        video = video,
                        onProgressClick = { onVideoProgress(video) },
                        onEditClick = { onVideoEdit(video) },
                        onDeleteClick = { onVideoDelete(video) },
                        onStatusChange = { onStatusChange(video.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsCards(uiState: com.example.ui.WatchUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Watching",
            value = uiState.watchingCount.toString(),
            color = StatusWatching,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "To Watch",
            value = uiState.toWatchCount.toString(),
            color = StatusToWatch,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Completed",
            value = uiState.completedCount.toString(),
            color = StatusWatched,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badgeCount: Int = 0,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(text = actionText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
