package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtpalaParkingApp(viewModel: ParkingViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val unsyncedCount by viewModel.unsyncedCountFlow.collectAsState()
    val isSyncing by viewModel.syncEngine.isSyncing.collectAsState()
    val statusMessage by viewModel.printStatusMessage.collectAsState()

    // Show Android Toasts when print/checkout operations take place
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PARK SATHI POS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                        Text(
                            text = if (isNepali) "पार्क साथी साास (SaaS Nepal System)" else "Nepal SaaS POS & Fiscal System",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // SaaS Cloud Sync Header Badge
                    CloudSyncHeaderBadge(
                        unsyncedCount = unsyncedCount,
                        isSyncing = isSyncing,
                        onSyncClick = { viewModel.triggerCloudSync() }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Language Switcher Toggle Header
                    LanguageToggleHeader(
                        isNepali = isNepali,
                        onLanguageToggle = { viewModel.setLanguageNepali(it) }
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Input, contentDescription = null) },
                    label = { Text(if (isNepali) "प्रवेश" else "Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label = { Text(if (isNepali) "निकास" else "Check-Out", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(if (isNepali) "इतिहास" else "History", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Paid, contentDescription = null) },
                    label = { Text(if (isNepali) "भुक्तानी" else "Payments", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    label = { Text(if (isNepali) "साास मल्टिगेट" else "SaaS Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(if (isNepali) "सेटिङ्स" else "Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "ScreenSwitch"
            ) { tab ->
                when (tab) {
                    0 -> EntryScreen(viewModel = viewModel)
                    1 -> CheckoutScreen(viewModel = viewModel)
                    2 -> HistoryScreen(viewModel = viewModel)
                    3 -> PaymentRecordsScreen(viewModel = viewModel)
                    4 -> SaaSAdminScreen(viewModel = viewModel)
                    5 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
