package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCoral
import com.example.ui.components.GlassCyan
import com.example.ui.components.GlassNavy
import com.example.ui.components.GlassStatBadge
import com.example.ui.components.GlassYellow
import com.example.ui.components.GraphDisplayMode
import com.example.ui.components.VibrationWaveformGraph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val reading by viewModel.currentReading.collectAsState()
    val buffer by viewModel.liveBuffer.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()
    val isTared by viewModel.isTared.collectAsState()
    val unit by viewModel.vibrationUnit.collectAsState()
    val graphMode by viewModel.graphDisplayMode.collectAsState()
    val sensorMode by viewModel.sensorMode.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    var sessionTitleInput by remember { mutableStateOf("") }

    val level = remember(reading.magnitude) {
        VibrationLevel.fromMagnitude(reading.magnitude)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_recording")
    val recPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_rec"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen lock & background notice banner when recording
        if (isRecording) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x25EF476F),
                    border = BorderStroke(1.dp, GlassCoral.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("recording_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(recPulse)
                                .clip(CircleShape)
                                .background(GlassCoral)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RECORDING IN BACKGROUND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = GlassCoral
                            )
                            Text(
                                text = "Sensors remain active when screen is turned off/locked",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Screen lock supported",
                            tint = GlassCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Real-Time Waveform Oscilloscope Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("waveform_card"),
                accentGlow = if (isRecording) GlassCoral else GlassCyan,
                isDark = isDark
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Mode selector toggle inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRecording) "RECORDING SENSOR DATA" else "LIVE VIBRATION FEED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (isRecording) GlassCoral else GlassCyan
                        )

                        // Mode switcher pills
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x301E2E4A) else Color(0x50E2E8F0))
                                .padding(2.dp)
                        ) {
                            GraphDisplayMode.values().forEach { mode ->
                                val selected = graphMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) {
                                                if (isDark) Color(0x6000F5D4) else Color(0xFF0284C7)
                                            } else Color.Transparent
                                        )
                                        .clickable { viewModel.setGraphDisplayMode(mode) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    VibrationWaveformGraph(
                        readings = buffer,
                        unit = unit,
                        displayMode = graphMode,
                        isDark = isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Live Intensity Gauge & Digital Readout
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_metrics_card"),
                isDark = isDark
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CURRENT INTENSITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = unit.formatValue(reading.magnitude),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 44.sp
                                    ),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = unit.abbreviation,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GlassCyan,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Qualitative Vibration Level Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(level.hexColor).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(level.hexColor).copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(level.hexColor))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = level.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(level.hexColor)
                                    )
                                }
                                Text(
                                    text = level.description,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Axis mini cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AxisMiniCard(label = "X AXIS", value = reading.x, unit = unit, color = Color(0xFFEF476F), modifier = Modifier.weight(1f), isDark = isDark)
                        AxisMiniCard(label = "Y AXIS", value = reading.y, unit = unit, color = Color(0xFF06D6A0), modifier = Modifier.weight(1f), isDark = isDark)
                        AxisMiniCard(label = "Z AXIS", value = reading.z, unit = unit, color = Color(0xFF00BBF9), modifier = Modifier.weight(1f), isDark = isDark)
                    }
                }
            }
        }

        // Live Statistics Grid (MAX, MIN, AVG, RMS)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isRecording) "SESSION STATISTICS" else "SESSION / WINDOW METRICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                val displayMin = if (isRecording) liveStats.minIntensity else (buffer.minOfOrNull { it.magnitude } ?: 0f)
                val displayMax = if (isRecording) liveStats.maxIntensity else (buffer.maxOfOrNull { it.magnitude } ?: 0f)
                val displayAvg = if (isRecording) liveStats.avgIntensity else (if (buffer.isNotEmpty()) buffer.map { it.magnitude }.average().toFloat() else 0f)
                val displayRms = if (isRecording) liveStats.rmsIntensity else (if (buffer.isNotEmpty()) kotlin.math.sqrt(buffer.map { it.magnitude * it.magnitude }.average()).toFloat() else 0f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassStatBadge(
                        label = "Max Peak",
                        value = unit.formatValue(displayMax),
                        unit = unit.abbreviation,
                        accentColor = GlassCoral,
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    GlassStatBadge(
                        label = "Average",
                        value = unit.formatValue(displayAvg),
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
                        label = "Minimum",
                        value = unit.formatValue(displayMin),
                        unit = unit.abbreviation,
                        accentColor = GlassCyan,
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    GlassStatBadge(
                        label = "RMS Intensity",
                        value = unit.formatValue(displayRms),
                        unit = unit.abbreviation,
                        accentColor = Color(0xFF00BBF9),
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                }
            }
        }

        // Recording Progress & Elapsed Time
        if (isRecording) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentGlow = GlassCoral,
                    isDark = isDark
                ) {
                    val secTotal = liveStats.elapsedMs / 1000
                    val min = secTotal / 60
                    val sec = secTotal % 60
                    val subSec = (liveStats.elapsedMs % 1000) / 100

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "LOGGING TIME",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d.%01d", min, sec, subSec),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GlassCoral
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SAMPLES LOGGED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "${liveStats.sampleCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }

        // Action Control Bar: Record & Tare
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tare / Calibrate Zero Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (isTared) viewModel.resetTare() else viewModel.tare()
                        }
                        .testTag("tare_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTared) {
                        if (isDark) Color(0x3500F5D4) else Color(0x300284C7)
                    } else {
                        if (isDark) Color(0x251E2E4A) else Color(0xDDFFFFFF)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isTared) GlassCyan.copy(alpha = 0.8f) else Color(0x25FFFFFF)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isTared) Icons.Default.RestartAlt else Icons.Default.Tune,
                            contentDescription = if (isTared) "Reset Tare" else "Tare Zero",
                            tint = if (isTared) GlassCyan else (if (isDark) Color.White else Color(0xFF334155)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isTared) "TARED (ZEROED)" else "TARE / ZERO",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isTared) GlassCyan else (if (isDark) Color.White else Color(0xFF334155))
                            )
                            Text(
                                text = if (isTared) "Tap to reset offset" else "Null resting drift",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Primary Record Button
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                sessionTitleInput = "Session " + SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date())
                                showStartDialog = true
                            }
                        }
                        .testTag("record_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isRecording) Color(0x40EF476F) else Color(0x3500F5D4),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            if (isRecording) listOf(GlassCoral, Color(0xFFFF5964)) else listOf(GlassCyan, Color(0xFF00BBF9))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Logging" else "Start Logging",
                            tint = if (isRecording) GlassCoral else GlassCyan,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(if (isRecording) recPulse else 1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isRecording) "STOP & SAVE LOG" else "START RECORDING",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isRecording) "Saves to CSV & DB" else "Continuous screen-off",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isRecording) GlassCoral else GlassCyan
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for session title
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = {
                Text(
                    text = "New Vibration Recording",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Vibrations will be measured and saved to database and CSV logs, even while the phone is locked in your pocket or vehicle mount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sessionTitleInput,
                        onValueChange = { sessionTitleInput = it },
                        label = { Text("Session Name / Tag") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartDialog = false
                        viewModel.startRecording(sessionTitleInput.ifBlank { "Vibration Log" })
                    },
                    modifier = Modifier.testTag("confirm_start_button")
                ) {
                    Text("Start Logging", color = GlassCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AxisMiniCard(
    label: String,
    value: Float,
    unit: VibrationUnit,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0x251E2E4A) else Color(0x60FFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%+.2f", value * unit.scaleFactor),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }
    }
}
