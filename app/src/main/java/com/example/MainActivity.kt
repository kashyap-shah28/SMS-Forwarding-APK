package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.SmsForwarderService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FiltersScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppScreen(val title: String) {
    DASHBOARD("SMS Forwarder"),
    FILTERS("Filter Rules"),
    LOGS("Audit Log"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    // State collections
    val emailSettings by viewModel.emailSettings.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val successCount by viewModel.successCount.collectAsStateWithLifecycle()
    val filteredCount by viewModel.filteredCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val allLogs by viewModel.logs.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val isTestingSmtp by viewModel.isTestingSmtp.collectAsStateWithLifecycle()
    val smtpTestResult by viewModel.smtpTestResult.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val simulationMessage by viewModel.simulationMessage.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Permissions handling
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = (permissions[Manifest.permission.RECEIVE_SMS] == true) &&
                (permissions[Manifest.permission.READ_SMS] == true)
        if (hasSmsPermission && emailSettings.isForwarderEnabled) {
            SmsForwarderService.startService(context)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Auto-prompt permissions on first launch if not granted
    LaunchedEffect(Unit) {
        if (!hasSmsPermission) {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (emailSettings.isForwarderEnabled) {
            SmsForwarderService.startService(context)
        }
    }

    // Back handling for custom navigation
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { currentScreen = AppScreen.DASHBOARD },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.FILTERS,
                    onClick = { currentScreen = AppScreen.FILTERS },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.FILTERS) Icons.Default.FilterList else Icons.Outlined.FilterList,
                            contentDescription = "Filters"
                        )
                    },
                    label = { Text("Filters") },
                    modifier = Modifier.testTag("nav_item_filters")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.LOGS,
                    onClick = { currentScreen = AppScreen.LOGS },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.LOGS) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Logs"
                        )
                    },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("nav_item_logs")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { currentScreen = AppScreen.SETTINGS },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .align(Alignment.TopCenter)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        emailSettings = emailSettings,
                        totalCount = totalCount,
                        successCount = successCount,
                        filteredCount = filteredCount,
                        failedCount = failedCount,
                        recentLogs = recentLogs,
                        isSimulating = isSimulating,
                        simulationMessage = simulationMessage,
                        onToggleForwarder = { enabled ->
                            viewModel.toggleForwarder(enabled)
                        },
                        onSimulateSms = { sender, message ->
                            viewModel.simulateIncomingSms(sender, message)
                        },
                        onDismissSimulationMessage = {
                            viewModel.clearSimulationMessage()
                        },
                        onNavigateToFilters = { currentScreen = AppScreen.FILTERS },
                        onNavigateToLogs = { currentScreen = AppScreen.LOGS },
                        onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                    )

                    AppScreen.FILTERS -> FiltersScreen(
                        rules = rules,
                        onAddRule = { rule -> viewModel.addRule(rule) },
                        onUpdateRule = { rule -> viewModel.updateRule(rule) },
                        onToggleRule = { rule -> viewModel.toggleRule(rule) },
                        onDeleteRule = { rule -> viewModel.deleteRule(rule) }
                    )

                    AppScreen.LOGS -> LogsScreen(
                        logs = allLogs,
                        onRetryForward = { log -> viewModel.retryForward(log) },
                        onClearAllLogs = { viewModel.clearAllLogs() }
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        settings = emailSettings,
                        isTestingSmtp = isTestingSmtp,
                        testSmtpResult = smtpTestResult,
                        hasSmsPermission = hasSmsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestSmsPermission = {
                            smsPermissionLauncher.launch(
                                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                            )
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onSaveSettings = { newSettings -> viewModel.saveEmailSettings(newSettings) },
                        onTestSmtp = { targetSettings -> viewModel.testSmtpConnection(targetSettings) },
                        onClearTestResult = { viewModel.clearSmtpTestResult() }
                    )
                }
            }
        }
    }
}
