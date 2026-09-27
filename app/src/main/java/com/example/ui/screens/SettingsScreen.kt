package com.example.ui.screens

import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SensorMode
import com.example.model.VibrationUnit
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCoral
import com.example.ui.components.GlassCyan

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val activeSensorMode by viewModel.sensorMode.collectAsState()
    val activeUnit by viewModel.vibrationUnit.collectAsState()
    val samplingDelayUs by viewModel.samplingDelayUs.collectAsState()
    val isTared by viewModel.isTared.collectAsState()

    var showClearDbDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SETTINGS & CALIBRATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "Configure sensor fusion, measurement units, and sampling frequency",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        // Background Recording with Screen Lock Explanation Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentGlow = GlassCyan,
                isDark = isDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Screen Lock",
                        tint = GlassCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Background & Locked Screen Logging",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "VibroLog operates as an Android Foreground Service with CPU Partial WakeLock. When you lock your phone or turn the screen off, sensor measurements and data logging continue without suspension. You can also view live vibration intensity directly in the notification bar.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // Measurement Units Selection
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MEASUREMENT UNIT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    VibrationUnit.values().forEach { unit ->
                        val selected = activeUnit == unit
                        UnitOptionRow(
                            title = unit.label,
                            subtitle = when (unit) {
                                VibrationUnit.METERS_PER_SEC_SQ -> "SI metric standard for vibration acceleration"
                                VibrationUnit.G_FORCE -> "Standard gravitational equivalent (1g = 9.81 m/s²)"
                                VibrationUnit.MILLIMETERS_PER_SEC_SQ -> "Standard mechanical & seismic vibration unit"
                            },
                            isSelected = selected,
                            onSelect = { viewModel.setVibrationUnit(unit) },
                            isDark = isDark
                        )
                        if (unit != VibrationUnit.values().last()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Sensor Mode Selection
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SENSOR SOURCE & PROCESSING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SensorMode.values().forEach { mode ->
                        val selected = activeSensorMode == mode
                        UnitOptionRow(
                            title = mode.displayName,
                            subtitle = mode.description,
                            isSelected = selected,
                            onSelect = { viewModel.setSensorMode(mode) },
                            isDark = isDark
                        )
                        if (mode != SensorMode.values().last()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Sampling Rate
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SAMPLING RATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val rates = listOf(
                        Triple(SensorManager.SENSOR_DELAY_FASTEST, "~100 Hz (Fastest)", "Highest resolution for rapid motor or machinery vibration"),
                        Triple(SensorManager.SENSOR_DELAY_GAME, "~50 Hz (Standard)", "Ideal balance of high accuracy and energy efficiency"),
                        Triple(SensorManager.SENSOR_DELAY_UI, "~20 Hz (Power Saver)", "Lower frequency suitable for extended multi-hour monitoring")
                    )

                    rates.forEachIndexed { index, (delay, title, desc) ->
                        val selected = samplingDelayUs == delay
                        UnitOptionRow(
                            title = title,
                            subtitle = desc,
                            isSelected = selected,
                            onSelect = { viewModel.setSamplingRate(delay) },
                            isDark = isDark
                        )
                        if (index < rates.size - 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Tare / Calibration Control
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SENSOR CALIBRATION (TARE ZERO)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tare sets the current phone orientation and resting noise floor to 0.0 m/s², subtracting any static tilt or engine mounting angle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassCyan.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.tare() }
                                .testTag("settings_tare_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTared) "Retare Current Position" else "Tare Zero Now",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlassCyan
                                )
                            }
                        }

                        if (isTared) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x251E2E4A) else Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.resetTare() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset Tare",
                                        tint = if (isDark) Color.White else Color(0xFF334155),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color.White else Color(0xFF334155)
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
private fun UnitOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            if (isDark) Color(0x3500F5D4) else Color(0x300284C7)
        } else {
            if (isDark) Color(0x181E2E4A) else Color(0x40FFFFFF)
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) GlassCyan.copy(alpha = 0.7f) else Color(0x15FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) GlassCyan else (if (isDark) Color.White else Color(0xFF0F172A))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = GlassCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
