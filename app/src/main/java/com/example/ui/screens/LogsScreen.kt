package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForwardLog
import com.example.data.model.LogStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<ForwardLog>,
    onRetryForward: (ForwardLog) -> Unit,
    onClearAllLogs: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<LogStatus?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, searchQuery, selectedStatusFilter) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                    log.sender.contains(searchQuery, ignoreCase = true) ||
                    log.messageBody.contains(searchQuery, ignoreCase = true) ||
                    (log.matchedRuleName?.contains(searchQuery, ignoreCase = true) == true)

            val matchesStatus = selectedStatusFilter == null || log.status == selectedStatusFilter

            matchesSearch && matchesStatus
        }
    }

    Column(modifier = Modifier.fillMaxSize().testTag("logs_screen")) {
        // Search and Actions Header
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Forwarding Audit Log",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All Logs", tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs by sender, body, or rule...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("search_logs_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text("All (${logs.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedStatusFilter == LogStatus.SUCCESS,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == LogStatus.SUCCESS) null else LogStatus.SUCCESS
                    },
                    label = { Text("Delivered", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedStatusFilter == LogStatus.FILTERED_OUT,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == LogStatus.FILTERED_OUT) null else LogStatus.FILTERED_OUT
                    },
                    label = { Text("Filtered", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedStatusFilter == LogStatus.FAILED,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == LogStatus.FAILED) null else LogStatus.FAILED
                    },
                    label = { Text("Failed", fontSize = 11.sp) }
                )
            }
        }

        // List of Logs
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedStatusFilter != null) {
                            "No logs match filter"
                        } else {
                            "No SMS logs recorded yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Incoming SMS messages will be captured and tracked here with full delivery details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogDetailCard(
                        log = log,
                        onRetry = { onRetryForward(log) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Logs") },
            text = { Text("Are you sure you want to delete all historical SMS forwarding logs? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllLogs()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LogDetailCard(
    log: ForwardLog,
    onRetry: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.receivedAt) { dateFormat.format(Date(log.receivedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_card_${log.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (log.status) {
                        LogStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.15f)
                        LogStatus.FILTERED_OUT -> MaterialTheme.colorScheme.surfaceVariant
                        LogStatus.FAILED -> ErrorRed.copy(alpha = 0.15f)
                        LogStatus.PENDING -> WarningAmber.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when (log.status) {
                            LogStatus.SUCCESS -> "DELIVERED"
                            LogStatus.FILTERED_OUT -> "FILTERED"
                            LogStatus.FAILED -> "FAILED"
                            LogStatus.PENDING -> "PENDING"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (log.status) {
                            LogStatus.SUCCESS -> SuccessGreen
                            LogStatus.FILTERED_OUT -> MaterialTheme.colorScheme.onSurfaceVariant
                            LogStatus.FAILED -> ErrorRed
                            LogStatus.PENDING -> WarningAmber
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = log.messageBody,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    if (!log.matchedRuleName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rule Matched: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.matchedRuleName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (!log.targetEmail.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Target Email: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.targetEmail,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (log.statusMessage.isNotBlank()) {
                        Text(
                            text = "Engine Response:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (log.status == LogStatus.FAILED) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (log.status == LogStatus.FAILED || log.status == LogStatus.FILTERED_OUT) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Forwarding to Inbox")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (expanded) "Show Less" else "Details")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
