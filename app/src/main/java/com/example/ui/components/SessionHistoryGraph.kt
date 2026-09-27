package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VibrationSampleEntity
import com.example.model.VibrationUnit
import kotlin.math.max

@Composable
fun SessionHistoryGraph(
    samples: List<VibrationSampleEntity>,
    maxIntensity: Float,
    avgIntensity: Float,
    minIntensity: Float,
    rmsIntensity: Float,
    modifier: Modifier = Modifier,
    unit: VibrationUnit = VibrationUnit.METERS_PER_SEC_SQ,
    isDark: Boolean = true
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val displaySamples = remember(samples) {
        // If there are many samples (e.g. 5000+), downsample gracefully for smooth 60fps rendering
        if (samples.size > 800) {
            val step = samples.size / 600
            samples.filterIndexed { index, _ -> index % step == 0 }
        } else {
            samples
        }
    }

    val selectedSample = remember(selectedIndex, displaySamples) {
        selectedIndex?.let { idx -> displaySamples.getOrNull(idx) }
    }

    val maxDisplayY = remember(maxIntensity) {
        max(1.5f, maxIntensity * 1.2f)
    }

    val peakSample = remember(displaySamples) {
        displaySamples.maxByOrNull { it.magnitude }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0x350A1120) else Color(0x90F8FAFC))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        if (isDark) Color(0x3500F5D4) else Color(0x300284C7),
                        Color(0x10FFFFFF)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Interactive header showing touched sample or overall stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedSample != null) {
                    val sec = selectedSample.elapsedMs / 1000f
                    Column {
                        Text(
                            text = String.format(java.util.Locale.US, "@ %.2fs: %s", sec, unit.format(selectedSample.magnitude)),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GlassCyan
                            )
                        )
                        Text(
                            text = String.format(
                                java.util.Locale.US,
                                "X: %.2f  Y: %.2f  Z: %.2f",
                                selectedSample.x,
                                selectedSample.y,
                                selectedSample.z
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = "SESSION WAVEFORM PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        )
                        Text(
                            text = "Touch/drag to inspect points",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Max and Avg badges
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Max: ${unit.format(maxIntensity)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassCoral
                        )
                    )
                    Text(
                        text = "Avg: ${unit.format(avgIntensity)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = GlassYellow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Chart with touch scrubber
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(displaySamples) {
                        detectTapGestures(
                            onPress = { offset ->
                                val count = displaySamples.size
                                if (count > 0) {
                                    val idx = ((offset.x / size.width) * (count - 1))
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    selectedIndex = idx
                                }
                            }
                        )
                    }
                    .pointerInput(displaySamples) {
                        detectDragGestures { change, _ ->
                            val count = displaySamples.size
                            if (count > 0) {
                                val idx = ((change.position.x / size.width) * (count - 1))
                                    .toInt()
                                    .coerceIn(0, count - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                if (width <= 0 || height <= 0 || displaySamples.isEmpty()) return@Canvas

                val count = displaySamples.size
                val xStep = width / max(1, count - 1).toFloat()

                // Average line (dashed yellow)
                val avgY = height - ((avgIntensity / maxDisplayY).coerceIn(0f, 1f) * height)
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                drawLine(
                    color = GlassYellow.copy(alpha = 0.5f),
                    start = Offset(0f, avgY),
                    end = Offset(width, avgY),
                    strokeWidth = 1.5f,
                    pathEffect = dash
                )

                // RMS line (dashed blue)
                if (rmsIntensity > 0f) {
                    val rmsY = height - ((rmsIntensity / maxDisplayY).coerceIn(0f, 1f) * height)
                    drawLine(
                        color = GlassBlue.copy(alpha = 0.45f),
                        start = Offset(0f, rmsY),
                        end = Offset(width, rmsY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Construct Waveform Path & Fill
                val path = Path()
                val fillPath = Path()
                var peakX = 0f
                var peakY = 0f
                var maxSampleMag = 0f

                for (i in displaySamples.indices) {
                    val s = displaySamples[i]
                    val x = i * xStep
                    val norm = (s.magnitude / maxDisplayY).coerceIn(0f, 1f)
                    val y = height - (norm * height)

                    if (s.magnitude > maxSampleMag) {
                        maxSampleMag = s.magnitude
                        peakX = x
                        peakY = y
                    }

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Gradient fill underneath curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassCyan.copy(alpha = 0.28f),
                            GlassCyan.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Waveform line
                drawPath(
                    path = path,
                    color = GlassCyan,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Highlight Peak point
                if (maxSampleMag > 0f) {
                    drawCircle(
                        color = GlassCoral,
                        radius = 5.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                }

                // Draw Scrubber Line if selected
                selectedIndex?.let { idx ->
                    if (idx in displaySamples.indices) {
                        val scrubX = idx * xStep
                        val sample = displaySamples[idx]
                        val scrubY = height - ((sample.magnitude / maxDisplayY).coerceIn(0f, 1f) * height)

                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(scrubX, 0f),
                            end = Offset(scrubX, height),
                            strokeWidth = 1.5f
                        )

                        drawCircle(
                            color = GlassCyan,
                            radius = 6.dp.toPx(),
                            center = Offset(scrubX, scrubY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(scrubX, scrubY)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Graph benchmark indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GlassYellow))
                    Text(
                        text = " Avg (${unit.format(avgIntensity)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GlassYellow)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GlassBlue))
                    Text(
                        text = " RMS (${unit.format(rmsIntensity)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GlassBlue)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GlassCoral))
                    Text(
                        text = " Peak",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GlassCoral)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${samples.size} samples",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}
