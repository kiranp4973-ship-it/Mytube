package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.WatchStatus
import com.example.data.model.WatchVideo
import com.example.data.remote.ParsedYouTubeVideo
import com.example.ui.theme.YouTubeRed
import kotlinx.coroutines.launch

@Composable
fun AddEditVideoDialog(
    existingVideo: WatchVideo?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (WatchVideo) -> Unit,
    onFetchMetadata: suspend (String) -> Result<ParsedYouTubeVideo>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlInput by remember { mutableStateOf(existingVideo?.videoUrl ?: "") }
    var titleInput by remember { mutableStateOf(existingVideo?.title ?: "") }
    var channelInput by remember { mutableStateOf(existingVideo?.channelTitle ?: "") }
    var thumbnailUrl by remember { mutableStateOf(existingVideo?.thumbnailUrl ?: "") }
    var videoId by remember { mutableStateOf(existingVideo?.videoId ?: "") }
    var categoryInput by remember { mutableStateOf(existingVideo?.category ?: "General") }
    var statusInput by remember { mutableStateOf(existingVideo?.watchStatus ?: WatchStatus.TO_WATCH) }
    var notesInput by remember { mutableStateOf(existingVideo?.notes ?: "") }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    val defaultCategories = remember(availableCategories) {
        (availableCategories.filter { it != "All" } + listOf("Tech", "Education", "Music", "Design", "General")).distinct()
    }

    fun triggerFetch(url: String) {
        if (url.isBlank()) return
        scope.launch {
            isFetching = true
            fetchError = null
            val result = onFetchMetadata(url)
            isFetching = false
            result.onSuccess { meta ->
                videoId = meta.videoId
                titleInput = meta.title
                channelInput = meta.channelTitle
                thumbnailUrl = meta.thumbnailUrl
            }.onFailure { err ->
                fetchError = err.message ?: "Could not parse YouTube URL"
            }
        }
    }

    // Auto-fetch if user pastes a valid URL in an empty add dialog
    LaunchedEffect(urlInput) {
        if (existingVideo == null && urlInput.contains("youtu") && videoId.isBlank()) {
            triggerFetch(urlInput)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingVideo == null) "Add YouTube Video" else "Edit Video",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // URL input row
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("YouTube URL or ID") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    trailingIcon = {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = { triggerFetch(urlInput) },
                                modifier = Modifier.testTag("fetch_metadata_btn")
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Auto-fetch",
                                    tint = YouTubeRed
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_url_input"),
                    singleLine = true
                )

                if (fetchError != null) {
                    Text(
                        text = fetchError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                // Thumbnail preview if loaded
                if (thumbnailUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Video Thumbnail Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Video Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_title_input"),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Channel
                OutlinedTextField(
                    value = channelInput,
                    onValueChange = { channelInput = it },
                    label = { Text("Channel / Creator") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Playlist / Category Selection
                Text(
                    text = "Playlist / Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultCategories.forEach { cat ->
                        FilterChip(
                            selected = categoryInput.equals(cat, ignoreCase = true),
                            onClick = { categoryInput = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                if (!showNewCategoryInput) {
                    TextButton(
                        onClick = { showNewCategoryInput = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Playlist", fontSize = 12.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            label = { Text("Custom Playlist") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customCategoryText.isNotBlank()) {
                                    categoryInput = customCategoryText.trim()
                                    showNewCategoryInput = false
                                }
                            }
                        ) {
                            Text("Use")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status
                Text(
                    text = "Watch Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatchStatus.entries.forEach { status ->
                        FilterChip(
                            selected = statusInput == status,
                            onClick = { statusInput = status },
                            label = { Text(status.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Personal Notes & Takeaways") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalId = existingVideo?.id.orEmpty()
                    val finalVideoId = videoId.ifBlank { "custom_" + System.currentTimeMillis() }
                    val finalThumbnail = thumbnailUrl.ifBlank {
                        "https://img.youtube.com/vi/$finalVideoId/hqdefault.jpg"
                    }
                    val video = WatchVideo(
                        id = finalId,
                        userId = existingVideo?.userId.orEmpty(),
                        videoId = finalVideoId,
                        videoUrl = urlInput.ifBlank { "https://www.youtube.com/watch?v=$finalVideoId" },
                        title = titleInput.ifBlank { "Saved YouTube Video" },
                        channelTitle = channelInput.ifBlank { "YouTube Creator" },
                        thumbnailUrl = finalThumbnail,
                        category = categoryInput.ifBlank { "General" },
                        status = statusInput.value,
                        progressSeconds = existingVideo?.progressSeconds ?: 0,
                        durationSeconds = existingVideo?.durationSeconds ?: 0,
                        lastWatchedAt = existingVideo?.lastWatchedAt ?: 0L,
                        notes = notesInput
                    )
                    onSave(video)
                },
                enabled = urlInput.isNotBlank() || titleInput.isNotBlank(),
                modifier = Modifier.testTag("save_video_confirm_btn")
            ) {
                Text(if (existingVideo == null) "Save Video" else "Update Video")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
