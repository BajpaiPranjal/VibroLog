package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VibrationReading
import com.example.model.VibrationUnit
import kotlin.math.max

enum class GraphDisplayMode(val label: String) {
    MAGNITUDE("Magnitude"),
    XYZ_AXES("3-Axis (X,Y,Z)")
}

val AxisColorX = Color(0xFFEF476F) // Coral / Red
val AxisColorY = Color(0xFF06D6A0) // Emerald / Green
val AxisColorZ = Color(0xFF00BBF9) // Cyan / Blue
val MagnitudeColor = Color(0xFF00F5D4) // Luminous Mint

@Composable
fun VibrationWaveformGraph(
    readings: List<VibrationReading>,
    modifier: Modifier = Modifier,
    unit: VibrationUnit = VibrationUnit.METERS_PER_SEC_SQ,
    displayMode: GraphDisplayMode = GraphDisplayMode.MAGNITUDE,
    isDark: Boolean = true
) {
    val peakValue by remember(readings) {
        derivedStateOf {
            readings.maxOfOrNull { it.magnitude } ?: 0f
        }
    }

    val maxDisplayY by remember(readings, displayMode) {
        derivedStateOf {
            if (displayMode == GraphDisplayMode.MAGNITUDE) {
                val maxMag = readings.maxOfOrNull { it.magnitude } ?: 1.0f
                max(2.5f, maxMag * 1.25f)
            } else {
                val maxAxis = readings.maxOfOrNull {
                    max(kotlin.math.abs(it.x), max(kotlin.math.abs(it.y), kotlin.math.abs(it.z)))
                } ?: 1.0f
                max(2.5f, maxAxis * 1.25f)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0x350A1120) else Color(0x90F8FAFC))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        if (isDark) Color(0x3000F5D4) else Color(0x400284C7),
                        Color(0x10FFFFFF)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar inside graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME OSCILLOSCOPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Peak: ${unit.format(peakValue)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MagnitudeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height

                if (width <= 0 || height <= 0 || readings.isEmpty()) return@Canvas

                val gridColor = if (isDark) Color(0x18FFFFFF) else Color(0x20000000)
                val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                if (displayMode == GraphDisplayMode.MAGNITUDE) {
                    // Magnitude mode: baseline is at the bottom (y = height)
                    // Draw 3 horizontal grid lines (25%, 50%, 75%)
                    val stepCount = 4
                    for (i in 1..stepCount) {
                        val y = height - (height * i / stepCount)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = gridDash
                        )
                    }

                    // Build path for magnitude
                    val path = Path()
                    val fillPath = Path()
                    val count = readings.size
                    val xStep = width / max(1, count - 1).toFloat()

                    var started = false
                    for (i in readings.indices) {
                        val reading = readings[i]
                        val x = i * xStep
                        val norm = (reading.magnitude / maxDisplayY).coerceIn(0f, 1f)
                        val y = height - (norm * height)

                        if (!started) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    if (started) {
                        fillPath.lineTo(width, height)
                        fillPath.close()

                        // Frosted neon glow fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MagnitudeColor.copy(alpha = 0.25f),
                                    MagnitudeColor.copy(alpha = 0.02f)
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Glowing stroke
                        drawPath(
                            path = path,
                            color = MagnitudeColor,
                            style = Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                } else {
                    // 3-Axis mode: center line is zero (y = height / 2)
                    val centerY = height / 2f
                    drawLine(
                        color = if (isDark) Color(0x35FFFFFF) else Color(0x40000000),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1.5f
                    )

                    // Draw +/- quarter lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, centerY - height * 0.35f),
                        end = Offset(width, centerY - height * 0.35f),
                        strokeWidth = 1f,
                        pathEffect = gridDash
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, centerY + height * 0.35f),
                        end = Offset(width, centerY + height * 0.35f),
                        strokeWidth = 1f,
                        pathEffect = gridDash
                    )

                    val pathX = Path()
                    val pathY = Path()
                    val pathZ = Path()
                    val count = readings.size
                    val xStep = width / max(1, count - 1).toFloat()

                    for (i in readings.indices) {
                        val r = readings[i]
                        val x = i * xStep
                        val yNormX = (r.x / maxDisplayY).coerceIn(-1f, 1f)
                        val yNormY = (r.y / maxDisplayY).coerceIn(-1f, 1f)
                        val yNormZ = (r.z / maxDisplayY).coerceIn(-1f, 1f)

                        val pyX = centerY - (yNormX * centerY * 0.9f)
                        val pyY = centerY - (yNormY * centerY * 0.9f)
                        val pyZ = centerY - (yNormZ * centerY * 0.9f)

                        if (i == 0) {
                            pathX.moveTo(x, pyX)
                            pathY.moveTo(x, pyY)
                            pathZ.moveTo(x, pyZ)
                        } else {
                            pathX.lineTo(x, pyX)
                            pathY.lineTo(x, pyY)
                            pathZ.lineTo(x, pyZ)
                        }
                    }

                    drawPath(pathX, AxisColorX, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(pathY, AxisColorY, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(pathZ, AxisColorZ, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displayMode == GraphDisplayMode.MAGNITUDE) {
                    LegendItem("Magnitude", MagnitudeColor)
                } else {
                    LegendItem("X Axis", AxisColorX)
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem("Y Axis", AxisColorY)
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem("Z Axis", AxisColorZ)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Range: ±${unit.format(maxDisplayY)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}
