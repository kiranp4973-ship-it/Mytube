package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseConnectionTestResult
import com.example.ui.theme.StatusWatched

@Composable
fun SupabaseSettingsDialog(
    currentUrl: String,
    currentAnonKey: String,
    currentWatchTable: String,
    currentUserTable: String,
    isTesting: Boolean,
    testResult: SupabaseConnectionTestResult?,
    onDismiss: () -> Unit,
    onTestConnection: (url: String, key: String, watchTable: String, userTable: String) -> Unit,
    onSaveConfig: (url: String, key: String, watchTable: String, userTable: String) -> Unit,
    onTriggerSync: () -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }
    var anonKeyInput by remember { mutableStateOf(currentAnonKey) }
    var watchTableInput by remember { mutableStateOf(currentWatchTable) }
    var userTableInput by remember { mutableStateOf(currentUserTable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supabase Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                Text(
                    text = "Connect to your Supabase project to sync your personal Watch List across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Project URL
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://your-project.supabase.co") },
                    leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Anon Key
                OutlinedTextField(
                    value = anonKeyInput,
                    onValueChange = { anonKeyInput = it },
                    label = { Text("Supabase Anon Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_key_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tables
                Text(
                    text = "Database Tables (Existing Schema)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = watchTableInput,
                    onValueChange = { watchTableInput = it },
                    label = { Text("Watch List Table") },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userTableInput,
                    onValueChange = { userTableInput = it },
                    label = { Text("User Manager Table") },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Button
                FilledTonalButton(
                    onClick = {
                        onTestConnection(urlInput, anonKeyInput, watchTableInput, userTableInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_supabase_btn"),
                    enabled = !isTesting && urlInput.isNotBlank() && anonKeyInput.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing Connection...")
                    } else {
                        Text("Test Connection & Schema")
                    }
                }

                // Test result banner
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = if (testResult.isSuccess) StatusWatched.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (testResult.isSuccess) StatusWatched else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = testResult.message,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (testResult.isSuccess) StatusWatched else MaterialTheme.colorScheme.error
                                )
                            }
                            if (testResult.isSuccess) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Latency: ${testResult.latencyMs}ms | Watch Table: ${testResult.watchTableStatus} | User Table: ${testResult.userTableStatus}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(urlInput, anonKeyInput, watchTableInput, userTableInput)
                    onTriggerSync()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_supabase_config_btn")
            ) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
