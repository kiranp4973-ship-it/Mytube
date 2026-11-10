package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.WatchVideo
import com.example.ui.theme.StatusWatched
import com.example.ui.theme.YouTubeRed

@Composable
fun PlaybackProgressDialog(
    video: WatchVideo,
    onDismiss: () -> Unit,
    onSaveProgress: (seconds: Int, duration: Int) -> Unit
) {
    val context = LocalContext.current
    val totalDuration = if (video.durationSeconds > 0) video.durationSeconds else 1200 // default 20m if unknown
    var currentSeconds by remember { mutableIntStateOf(video.progressSeconds) }
    var sliderValue by remember { mutableFloatStateOf(currentSeconds.toFloat()) }

    fun formatTime(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        val h = m / 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m % 60, s)
        } else {
            String.format("%02d:%02d", m, s)
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
                    text = "Track Playback Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Video header card with thumbnail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(video.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                        Text(
                            text = video.channelTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Watch on YouTube button
                Button(
                    onClick = {
                        val timestampParam = if (currentSeconds > 0) "&t=${currentSeconds}s" else ""
                        val intentUrl = if (video.videoUrl.contains("?")) {
                            "${video.videoUrl}$timestampParam"
                        } else {
                            "${video.videoUrl}?$timestampParam"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_in_youtube_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentSeconds > 0) "Resume at ${formatTime(currentSeconds)} on YouTube" else "Watch on YouTube",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Time counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current: ${formatTime(currentSeconds)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Total: ${formatTime(totalDuration)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                // Scrubber Slider
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        currentSeconds = it.toInt()
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_progress_slider")
                )

                // Quick Jump buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            currentSeconds = (currentSeconds - 60).coerceAtLeast(0)
                            sliderValue = currentSeconds.toFloat()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-1m", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            currentSeconds = (currentSeconds + 60).coerceAtMost(totalDuration)
                            sliderValue = currentSeconds.toFloat()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1m", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            currentSeconds = (currentSeconds + 300).coerceAtMost(totalDuration)
                            sliderValue = currentSeconds.toFloat()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+5m", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            currentSeconds = totalDuration
                            sliderValue = totalDuration.toFloat()
                        },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusWatched)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Finished", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveProgress(currentSeconds, totalDuration) },
                modifier = Modifier.testTag("save_progress_btn")
            ) {
                Text("Save Position")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
