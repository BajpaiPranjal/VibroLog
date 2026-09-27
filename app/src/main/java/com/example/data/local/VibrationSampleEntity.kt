package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual recorded data point for a vibration session.
 */
@Entity(
    tableName = "vibration_samples",
    foreignKeys = [
        ForeignKey(
            entity = VibrationSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId", "elapsedMs"])]
)
data class VibrationSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sessionId")
    val sessionId: Long,
    val timestamp: Long,
    val elapsedMs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float
)
