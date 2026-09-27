package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AmbientGlassBackground
import com.example.ui.components.GlassCoral
import com.example.ui.components.GlassCyan
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.SessionDetailSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var darkThemeEnabled by remember { mutableStateOf(true) }

            // Handle Android 13+ Notification Permission for Background Foreground Service
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { /* User responded */ }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Handle UI events (CSV sharing intent, toast messages)
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.ShowToast -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                        }
                        is UiEvent.ShareCsv -> {
                            context.startActivity(event.intent)
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = darkThemeEnabled) {
                VibroLogApp(
                    viewModel = viewModel,
                    darkTheme = darkThemeEnabled,
                    onToggleDarkTheme = { darkThemeEnabled = !darkThemeEnabled }
                )
            }
        }
    }
}

@Composable
fun VibroLogApp(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()

    // Handle system back navigation
    BackHandler(enabled = selectedSession != null || currentTab != 0) {
        if (selectedSession != null) {
            viewModel.clearSelectedSession()
        } else if (currentTab != 0) {
            viewModel.setTab(0)
        }
    }

    AmbientGlassBackground(isDark = darkTheme) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VibroLogTopBar(
                    isRecording = isRecording,
                    elapsedMs = liveStats.elapsedMs,
                    isDark = darkTheme,
                    onToggleTheme = onToggleDarkTheme
                )
            },
            bottomBar = {
                FrostedGlassNavBar(
                    selectedTab = currentTab,
                    onSelectTab = { viewModel.setTab(it) },
                    isDark = darkTheme
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when (currentTab) {
                    0 -> MonitorScreen(viewModel = viewModel, isDark = darkTheme)
                    1 -> HistoryScreen(viewModel = viewModel, isDark = darkTheme)
                    2 -> SettingsScreen(viewModel = viewModel, isDark = darkTheme)
                }

                // Overlay Session Details when user taps a session
                AnimatedVisibility(
                    visible = selectedSession != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    SessionDetailSheet(
                        viewModel = viewModel,
                        isDark = darkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun VibroLogTopBar(
    isRecording: Boolean,
    elapsedMs: Long,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = GlassCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Logo",
                            tint = GlassCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vibro",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Log",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = GlassCyan
                        )
                    }
                    Text(
                        text = "PRECISION VIBRATION ANALYZER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    val secTotal = elapsedMs / 1000
                    val min = secTotal / 60
                    val sec = secTotal % 60
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x35EF476F),
                        border = BorderStroke(1.dp, GlassCoral.copy(alpha = 0.7f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GlassCoral)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d REC", min, sec),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassCoral
                                )
                            )
                        }
                    }
                }

                // Dark/Light Theme toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.size(36.dp).testTag("theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FrostedGlassNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xD80E1729) else Color(0xE8FFFFFF),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        if (isDark) Color(0x4000F5D4) else Color(0x300284C7),
                        Color(0x18FFFFFF)
                    )
                )
            ),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    title = "Live Monitor",
                    icon = Icons.Default.ShowChart,
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    testTag = "nav_monitor_tab",
                    isDark = isDark
                )
                NavBarItem(
                    title = "History & Logs",
                    icon = Icons.Default.History,
                    selected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    testTag = "nav_history_tab",
                    isDark = isDark
                )
                NavBarItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    testTag = "nav_settings_tab",
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isDark: Boolean
) {
    val activeColor = GlassCyan
    val inactiveColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            if (isDark) Color(0x2800F5D4) else Color(0x200284C7)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
        }
    }
}
