package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmailSettings
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    settings: EmailSettings,
    isTestingSmtp: Boolean,
    testSmtpResult: String?,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSaveSettings: (EmailSettings) -> Unit,
    onTestSmtp: (EmailSettings) -> Unit,
    onClearTestResult: () -> Unit
) {
    val context = LocalContext.current

    var smtpHost by remember(settings.smtpHost) { mutableStateOf(settings.smtpHost) }
    var smtpPort by remember(settings.smtpPort) { mutableIntStateOf(settings.smtpPort) }
    var smtpUser by remember(settings.smtpUser) { mutableStateOf(settings.smtpUser) }
    var smtpPassword by remember(settings.smtpPassword) { mutableStateOf(settings.smtpPassword) }
    var senderDisplayName by remember(settings.senderDisplayName) { mutableStateOf(settings.senderDisplayName) }
    var defaultRecipient by remember(settings.defaultRecipient) { mutableStateOf(settings.defaultRecipient) }
    var encryptionType by remember(settings.encryptionType) { mutableStateOf(settings.encryptionType) }
    var forwardAllByDefault by remember(settings.forwardAllByDefault) { mutableStateOf(settings.forwardAllByDefault) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGmailGuide by remember { mutableStateOf(false) }

    fun currentEditedSettings(): EmailSettings {
        return settings.copy(
            smtpHost = smtpHost.trim(),
            smtpPort = smtpPort,
            smtpUser = smtpUser.trim(),
            smtpPassword = smtpPassword.trim(),
            senderDisplayName = senderDisplayName.trim(),
            defaultRecipient = defaultRecipient.trim(),
            encryptionType = encryptionType,
            forwardAllByDefault = forwardAllByDefault
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recipient Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Forwarding Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Where forwarded SMS messages should be sent by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultRecipient,
                        onValueChange = {
                            defaultRecipient = it
                            onSaveSettings(currentEditedSettings())
                        },
                        label = { Text("Default Inbox Email *") },
                        placeholder = { Text("your.email@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_recipient_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Forward All Unmatched SMS",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "If enabled, messages not matching any filter rule are still forwarded to your default inbox.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = forwardAllByDefault,
                            onCheckedChange = {
                                forwardAllByDefault = it
                                onSaveSettings(currentEditedSettings())
                            },
                            modifier = Modifier.testTag("forward_all_switch")
                        )
                    }
                }
            }
        }

        // SMTP Server Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SMTP Delivery Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure your email provider to send SMS straight to your inbox.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = smtpHost == "smtp.gmail.com",
                            onClick = {
                                smtpHost = "smtp.gmail.com"
                                smtpPort = 587
                                encryptionType = "STARTTLS"
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("Gmail", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = smtpHost == "smtp.office365.com",
                            onClick = {
                                smtpHost = "smtp.office365.com"
                                smtpPort = 587
                                encryptionType = "STARTTLS"
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("Outlook", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = smtpHost == "smtp.mail.yahoo.com",
                            onClick = {
                                smtpHost = "smtp.mail.yahoo.com"
                                smtpPort = 465
                                encryptionType = "SSL"
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("Yahoo", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = smtpHost,
                            onValueChange = {
                                smtpHost = it
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("SMTP Host") },
                            placeholder = { Text("smtp.gmail.com") },
                            singleLine = true,
                            modifier = Modifier.weight(2f).testTag("smtp_host_input")
                        )

                        OutlinedTextField(
                            value = smtpPort.toString(),
                            onValueChange = {
                                smtpPort = it.toIntOrNull() ?: 587
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("smtp_port_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = encryptionType == "STARTTLS",
                            onClick = {
                                encryptionType = "STARTTLS"
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("STARTTLS (587)") }
                        )
                        FilterChip(
                            selected = encryptionType == "SSL",
                            onClick = {
                                encryptionType = "SSL"
                                onSaveSettings(currentEditedSettings())
                            },
                            label = { Text("SSL/TLS (465)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = smtpUser,
                        onValueChange = {
                            smtpUser = it
                            onSaveSettings(currentEditedSettings())
                        },
                        label = { Text("SMTP Username / Email") },
                        placeholder = { Text("sender@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("smtp_user_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = smtpPassword,
                        onValueChange = {
                            smtpPassword = it
                            onSaveSettings(currentEditedSettings())
                        },
                        label = { Text("SMTP App Password") },
                        placeholder = { Text("16-character app password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("smtp_password_input")
                    )

                    // Helper for Gmail App Passwords
                    TextButton(
                        onClick = { showGmailGuide = !showGmailGuide },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showGmailGuide) "Hide App Password Guide" else "How to get a Gmail App Password?", fontSize = 12.sp)
                    }

                    AnimatedVisibility(visible = showGmailGuide) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Google App Password Instructions:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "1. Go to myaccount.google.com -> Security\n" +
                                            "2. Ensure 2-Step Verification is ON\n" +
                                            "3. Search for 'App passwords'\n" +
                                            "4. Name it 'SMS Forwarder' and copy the 16-character code here.\n" +
                                            "(Do NOT use your personal Google account login password)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val current = currentEditedSettings()
                            onSaveSettings(current)
                            onTestSmtp(current)
                        },
                        enabled = !isTestingSmtp,
                        modifier = Modifier.fillMaxWidth().testTag("test_smtp_btn")
                    ) {
                        if (isTestingSmtp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing SMTP Connection...")
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test SMTP Connection")
                        }
                    }

                    if (testSmtpResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (testSmtpResult.startsWith("Connected")) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (testSmtpResult.startsWith("Connected")) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (testSmtpResult.startsWith("Connected")) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = testSmtpResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (testSmtpResult.startsWith("Connected")) SuccessGreen else ErrorRed,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearTestResult, modifier = Modifier.size(24.dp)) {
                                    Text("✕", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Permissions & Background Reliability Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reliability & Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ensure the app can capture SMS and stay alive continuously in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // SMS Permission Item
                    PermissionStatusRow(
                        title = "SMS Capture Permission",
                        description = "Required to intercept incoming text messages",
                        isGranted = hasSmsPermission,
                        icon = Icons.Default.Sms,
                        onRequest = onRequestSmsPermission
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notification Permission Item
                    PermissionStatusRow(
                        title = "Ongoing Notification Permission",
                        description = "Required for Android Foreground Service to avoid being terminated by OS",
                        isGranted = hasNotificationPermission,
                        icon = Icons.Default.Notifications,
                        onRequest = onRequestNotificationPermission
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Battery Optimization Bypass Button
                    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
                    val isIgnoringBattery = remember {
                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isIgnoringBattery) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Battery Optimization",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isIgnoringBattery) {
                                            "Unrestricted battery: Android will not kill the background forwarder service."
                                        } else {
                                            "Android Doze mode may throttle background tasks. Allow unrestricted battery usage for 100% reliable instant forwarding."
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isIgnoringBattery) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(fallback)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Disable Battery Throttling", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SuccessGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "GRANTED",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            Button(
                onClick = onRequest,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}
