package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VibrationSessionEntity
import com.example.model.VibrationUnit
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCoral
import com.example.ui.components.GlassCyan
import com.example.ui.components.GlassYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val sessions by viewModel.sessions.collectAsState()
    val unit by viewModel.vibrationUnit.collectAsState()
    var sessionToDelete by remember { mutableStateOf<VibrationSessionEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RECORDED SESSIONS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${sessions.size} sessions recorded with statistics & CSV logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("empty_history_card"),
                    isDark = isDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GlassCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = GlassCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Vibration Logs Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start recording in the Monitor tab. Sessions record even when the screen is locked and can be exported as CSV.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassCyan.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setTab(0) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = GlassCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Go to Live Monitor",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlassCyan
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionCardItem(
                    session = session,
                    unit = unit,
                    onInspect = { viewModel.selectSession(session.id) },
                    onExportCsv = { viewModel.exportSessionCsv(session.id) },
                    onDelete = { sessionToDelete = session },
                    isDark = isDark
                )
            }
        }
    }

    // Delete confirmation dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete \"${session.title}\"? All ${session.sampleCount} recorded data points will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = GlassCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionCardItem(
    session: VibrationSessionEntity,
    unit: VibrationUnit,
    onInspect: () -> Unit,
    onExportCsv: () -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean
) {
    val dateStr = remember(session.startTime) {
        SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.US).format(Date(session.startTime))
    }

    val durationStr = remember(session.durationMs) {
        val totalSec = session.durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onInspect)
            .testTag("session_card_${session.id}"),
        isDark = isDark
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateStr • Duration: $durationStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x301E2E4A) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "${session.sampleCount} pts",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = GlassCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Mini Stat Boxes: Max, Avg, Min, RMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatBox(
                    label = "MAX",
                    value = unit.formatValue(session.maxIntensity),
                    color = GlassCoral,
                    unit = unit.abbreviation,
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
                MiniStatBox(
                    label = "AVG",
                    value = unit.formatValue(session.avgIntensity),
                    color = GlassYellow,
                    unit = unit.abbreviation,
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
                MiniStatBox(
                    label = "MIN",
                    value = unit.formatValue(session.minIntensity),
                    color = GlassCyan,
                    unit = unit.abbreviation,
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
                MiniStatBox(
                    label = "RMS",
                    value = unit.formatValue(session.rmsIntensity),
                    color = Color(0xFF00BBF9),
                    unit = unit.abbreviation,
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Export CSV Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x2500F5D4),
                    border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onExportCsv)
                        .testTag("export_csv_btn_${session.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = GlassCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlassCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // View Details Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x251E2E4A) else Color(0xDDFFFFFF),
                    border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onInspect)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Inspect",
                            tint = if (isDark) Color.White else Color(0xFF334155),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inspect Graph",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color(0xFF334155)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Icon Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_btn_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatBox(
    label: String,
    value: String,
    color: Color,
    unit: String,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0x201E2E4A) else Color(0x80FFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }
    }
}
