package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VibrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: VibrationSessionEntity): Long

    @Update
    suspend fun updateSession(session: VibrationSessionEntity)

    @Query("DELETE FROM vibration_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM vibration_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<VibrationSessionEntity>>

    @Query("SELECT * FROM vibration_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): VibrationSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<VibrationSampleEntity>)

    @Query("SELECT * FROM vibration_samples WHERE sessionId = :sessionId ORDER BY elapsedMs ASC")
    suspend fun getSamplesForSession(sessionId: Long): List<VibrationSampleEntity>

    @Query("SELECT * FROM vibration_samples WHERE sessionId = :sessionId ORDER BY elapsedMs ASC")
    fun getSamplesForSessionFlow(sessionId: Long): Flow<List<VibrationSampleEntity>>

    @Query("SELECT COUNT(*) FROM vibration_samples WHERE sessionId = :sessionId")
    suspend fun getSampleCountForSession(sessionId: Long): Int

    @Query("DELETE FROM vibration_sessions")
    suspend fun deleteAllSessions()
}
