package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted summary metadata of a recorded vibration session.
 */
@Entity(tableName = "vibration_sessions")
data class VibrationSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val sampleCount: Int,
    val minIntensity: Float,
    val maxIntensity: Float,
    val avgIntensity: Float,
    val rmsIntensity: Float,
    val sensorType: String,
    val samplingRateHz: Int,
    val notes: String = ""
)
