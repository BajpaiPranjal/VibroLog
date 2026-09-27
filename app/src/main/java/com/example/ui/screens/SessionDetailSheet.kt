package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.model.VibrationLevel
import com.example.model.VibrationUnit
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCoral
import com.example.ui.components.GlassCyan
import com.example.ui.components.GlassStatBadge
import com.example.ui.components.GlassYellow
import com.example.ui.components.SessionHistoryGraph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailSheet(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val session by viewModel.selectedSession.collectAsState()
    val samples by viewModel.selectedSessionSamples.collectAsState()
    val isLoading by viewModel.isLoadingDetail.collectAsState()
    val unit by viewModel.vibrationUnit.collectAsState()

    if (session == null) return

    val currentSession = session!!

    val dateStr = remember(currentSession.startTime) {
        SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.US).format(Date(currentSession.startTime))
    }

    val durationStr = remember(currentSession.durationMs) {
        val totalSec = currentSession.durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val ms = (currentSession.durationMs % 1000) / 10
        String.format(Locale.US, "%02d:%02d.%02d", min, sec, ms)
    }

    val peakLevel = remember(currentSession.maxIntensity) {
        VibrationLevel.fromMagnitude(currentSession.maxIntensity)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("session_detail_screen"),
        color = if (isDark) Color(0xF2070B16) else Color(0xF5F8FAFC)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.clearSelectedSession() },
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSession.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = { viewModel.deleteSession(currentSession.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GlassCoral
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GlassCyan)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full Interactive Waveform Chart
                    item {
                        SessionHistoryGraph(
                            samples = samples,
                            maxIntensity = currentSession.maxIntensity,
                            avgIntensity = currentSession.avgIntensity,
                            minIntensity = currentSession.minIntensity,
                            rmsIntensity = currentSession.rmsIntensity,
                            unit = unit,
                            isDark = isDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }

                    // Qualitative Peak Level Indicator Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentGlow = Color(peakLevel.hexColor),
                            isDark = isDark
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(peakLevel.hexColor).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(peakLevel.hexColor)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(peakLevel.hexColor))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PEAK INTENSITY CLASSIFICATION",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(peakLevel.hexColor)
                                    )
                                    Text(
                                        text = peakLevel.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = peakLevel.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Key Statistics Summary Grid (Max, Min, Avg, RMS)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SESSION STATISTICS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassStatBadge(
                                    label = "Max Intensity",
                                    value = unit.formatValue(currentSession.maxIntensity),
                                    unit = unit.abbreviation,
                                    accentColor = GlassCoral,
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                                GlassStatBadge(
                                    label = "Average",
                                    value = unit.formatValue(currentSession.avgIntensity),
                                    unit = unit.abbreviation,
                                    accentColor = GlassYellow,
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassStatBadge(
                                    label = "Min Intensity",
                                    value = unit.formatValue(currentSession.minIntensity),
                                    unit = unit.abbreviation,
                                    accentColor = GlassCyan,
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                                GlassStatBadge(
                                    label = "RMS Vibration",
                                    value = unit.formatValue(currentSession.rmsIntensity),
                                    unit = unit.abbreviation,
                                    accentColor = Color(0xFF00BBF9),
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                            }
                        }
                    }

                    // Recording Details Breakdown Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isDark = isDark
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "CAPTURE METRICS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                DetailRow(label = "Total Duration", value = durationStr, isDark = isDark)
                                DetailRow(label = "Sample Count", value = "${currentSession.sampleCount} data points", isDark = isDark)
                                DetailRow(label = "Effective Sampling Rate", value = "~${currentSession.samplingRateHz} Hz", isDark = isDark)
                                DetailRow(label = "Sensor Used", value = currentSession.sensorType, isDark = isDark)
                            }
                        }
                    }

                    // Big Prominent Export CSV Button
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.exportSessionCsv(currentSession.id) }
                                .testTag("export_csv_action_btn"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x3500F5D4),
                            border = BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(listOf(GlassCyan, Color(0xFF00BBF9)))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export CSV",
                                    tint = GlassCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "EXPORT CSV DATA FILE",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Raw 3-axis (X, Y, Z), magnitude & timestamps",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = GlassCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}
