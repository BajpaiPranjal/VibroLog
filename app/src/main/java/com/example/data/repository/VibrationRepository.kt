package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.VibrationDao
import com.example.data.local.VibrationSampleEntity
import com.example.data.local.VibrationSessionEntity
import com.example.model.VibrationLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VibrationRepository(
    private val vibrationDao: VibrationDao,
    private val context: Context
) {
    val allSessions: Flow<List<VibrationSessionEntity>> = vibrationDao.getAllSessions()

    suspend fun getSessionById(id: Long): VibrationSessionEntity? = withContext(Dispatchers.IO) {
        vibrationDao.getSessionById(id)
    }

    suspend fun getSamplesForSession(sessionId: Long): List<VibrationSampleEntity> = withContext(Dispatchers.IO) {
        vibrationDao.getSamplesForSession(sessionId)
    }

    fun getSamplesForSessionFlow(sessionId: Long): Flow<List<VibrationSampleEntity>> {
        return vibrationDao.getSamplesForSessionFlow(sessionId)
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        vibrationDao.deleteSession(sessionId)
    }

    suspend fun deleteAllSessions() = withContext(Dispatchers.IO) {
        vibrationDao.deleteAllSessions()
    }

    /**
     * Exports the session and its samples to a CSV file in cache, returning the content Uri for sharing.
     */
    suspend fun exportSessionToCsv(sessionId: Long): ExportResult = withContext(Dispatchers.IO) {
        val session = vibrationDao.getSessionById(sessionId)
            ?: return@withContext ExportResult.Error("Session not found")
        val samples = vibrationDao.getSamplesForSession(sessionId)

        try {
            val exportDir = File(context.cacheDir, "vibration_logs")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val sanitizedTitle = session.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "VibroLog_${sanitizedTitle}_${session.startTime}.csv"
            val file = File(exportDir, fileName)

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(session.startTime))

            FileWriter(file).use { writer ->
                // Metadata Header
                writer.append("# VibroLog Vibration Sensor Recording Log\n")
                writer.append("# Title: ${session.title}\n")
                writer.append("# Recorded At: $readableDate\n")
                writer.append("# Duration (ms): ${session.durationMs}\n")
                writer.append("# Duration (formatted): ${formatDuration(session.durationMs)}\n")
                writer.append("# Total Samples: ${samples.size}\n")
                writer.append("# Sensor Mode: ${session.sensorType}\n")
                writer.append("# Target Sampling Rate: ~${session.samplingRateHz} Hz\n")
                writer.append(String.format(Locale.US, "# Statistics - Min Intensity: %.4f m/s²\n", session.minIntensity))
                writer.append(String.format(Locale.US, "# Statistics - Max Intensity: %.4f m/s²\n", session.maxIntensity))
                writer.append(String.format(Locale.US, "# Statistics - Avg Intensity: %.4f m/s²\n", session.avgIntensity))
                writer.append(String.format(Locale.US, "# Statistics - RMS Intensity: %.4f m/s²\n", session.rmsIntensity))
                if (session.notes.isNotBlank()) {
                    writer.append("# Notes: ${session.notes.replace("\n", " ")}\n")
                }
                writer.append("#\n")

                // CSV Column Headers
                writer.append("Index,Timestamp_Epoch_ms,Elapsed_ms,DateTime_ISO,Accel_X_ms2,Accel_Y_ms2,Accel_Z_ms2,Magnitude_ms2,Intensity_Level\n")

                // Sample rows
                for ((index, sample) in samples.withIndex()) {
                    val dateIso = isoFormat.format(Date(sample.timestamp))
                    val level = VibrationLevel.fromMagnitude(sample.magnitude).name
                    writer.append(
                        String.format(
                            Locale.US,
                            "%d,%d,%d,%s,%.4f,%.4f,%.4f,%.4f,%s\n",
                            index + 1,
                            sample.timestamp,
                            sample.elapsedMs,
                            dateIso,
                            sample.x,
                            sample.y,
                            sample.z,
                            sample.magnitude,
                            level
                        )
                    )
                }
                writer.flush()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            ExportResult.Success(file, uri, fileName, samples.size)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Failed to generate CSV file")
        }
    }

    /**
     * Creates an Intent to share the CSV via Android share sheet.
     */
    fun createShareIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "VibroLog Vibration Recording: $fileName")
            putExtra(Intent.EXTRA_TEXT, "Exported vibration measurement data from VibroLog.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val ms = (durationMs % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", min, sec, ms)
    }

    sealed class ExportResult {
        data class Success(
            val file: File,
            val uri: Uri,
            val fileName: String,
            val sampleCount: Int
        ) : ExportResult()

        data class Error(val message: String) : ExportResult()
    }

    companion object {
        @Volatile
        private var INSTANCE: VibrationRepository? = null

        fun getInstance(context: Context): VibrationRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = VibrationRepository(db.vibrationDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
