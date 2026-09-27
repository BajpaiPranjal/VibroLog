package com.example.model

import kotlin.math.sqrt

/**
 * Represents a single instantaneous 3-axis vibration reading.
 */
data class VibrationReading(
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedMs: Long = 0L,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = calculateMagnitude(x, y, z)
) {
    companion object {
        fun calculateMagnitude(x: Float, y: Float, z: Float): Float {
            return sqrt(x * x + y * y + z * z)
        }
    }
}

/**
 * Display units for vibration measurements.
 */
enum class VibrationUnit(val label: String, val scaleFactor: Float, val abbreviation: String) {
    METERS_PER_SEC_SQ("m/s²", 1.0f, "m/s²"),
    G_FORCE("g", 1.0f / 9.80665f, "g"),
    MILLIMETERS_PER_SEC_SQ("mm/s²", 1000.0f, "mm/s²");

    fun format(valueInMetersPerSecSq: Float): String {
        val converted = valueInMetersPerSecSq * scaleFactor
        return when (this) {
            METERS_PER_SEC_SQ -> String.format(java.util.Locale.US, "%.2f %s", converted, abbreviation)
            G_FORCE -> String.format(java.util.Locale.US, "%.3f %s", converted, abbreviation)
            MILLIMETERS_PER_SEC_SQ -> String.format(java.util.Locale.US, "%.0f %s", converted, abbreviation)
        }
    }

    fun formatValue(valueInMetersPerSecSq: Float): String {
        val converted = valueInMetersPerSecSq * scaleFactor
        return when (this) {
            METERS_PER_SEC_SQ -> String.format(java.util.Locale.US, "%.2f", converted)
            G_FORCE -> String.format(java.util.Locale.US, "%.3f", converted)
            MILLIMETERS_PER_SEC_SQ -> String.format(java.util.Locale.US, "%.0f", converted)
        }
    }
}

/**
 * Qualitative vibration intensity levels (similar to Modified Mercalli / ISO mechanical vibration levels).
 */
enum class VibrationLevel(
    val title: String,
    val minThreshold: Float,
    val hexColor: Long,
    val description: String
) {
    CALM("Calm / Resting", 0.0f, 0xFF00F5D4, "Imperceptible resting state"),
    LIGHT("Light Vibration", 0.35f, 0xFF00BBF9, "Noticeable gentle trembling or hum"),
    MODERATE("Moderate Shake", 1.20f, 0xFFFEE440, "Clear mechanical or vehicle vibration"),
    STRONG("Strong Shake", 3.00f, 0xFFF77F00, "Vigorous movement or strong jolting"),
    SEVERE("Severe Impact", 6.00f, 0xFFEF476F, "High g-force impact or turbulent shock");

    companion object {
        fun fromMagnitude(mag: Float): VibrationLevel {
            return when {
                mag >= SEVERE.minThreshold -> SEVERE
                mag >= STRONG.minThreshold -> STRONG
                mag >= MODERATE.minThreshold -> MODERATE
                mag >= LIGHT.minThreshold -> LIGHT
                else -> CALM
            }
        }
    }
}

/**
 * Sensor source mode used for vibration capture.
 */
enum class SensorMode(val displayName: String, val description: String) {
    LINEAR_ACCELERATION(
        "Linear Acceleration (Sensor Fusion)",
        "Hardware-filtered acceleration isolating pure vibration by automatically subtracting gravity."
    ),
    RAW_ACCELEROMETER(
        "Accelerometer (High-Pass Filter)",
        "Raw accelerometer data processed through a real-time DC removal filter."
    )
}
