package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.VibrationSampleEntity
import com.example.data.local.VibrationSessionEntity
import com.example.model.SensorMode
import com.example.model.VibrationReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class LiveSessionStats(
    val elapsedMs: Long = 0L,
    val sampleCount: Int = 0,
    val minIntensity: Float = 0f,
    val maxIntensity: Float = 0f,
    val avgIntensity: Float = 0f,
    val rmsIntensity: Float = 0f,
    val currentIntensity: Float = 0f
)

/**
 * Singleton controller managing the sensor listener, calibration/tare,
 * rolling display buffer, and background recording lifecycle.
 */
class VibrationRecorderManager private constructor(private val appContext: Context) : SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.vibrationDao()

    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var activeSensor: Sensor? = null

    // State flows
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentReading = MutableStateFlow(VibrationReading())
    val currentReading: StateFlow<VibrationReading> = _currentReading.asStateFlow()

    // Rolling window buffer of recent readings for 60fps graph (retains last ~250 points)
    private val _liveBuffer = MutableStateFlow<List<VibrationReading>>(emptyList())
    val liveBuffer: StateFlow<List<VibrationReading>> = _liveBuffer.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveSessionStats())
    val liveStats: StateFlow<LiveSessionStats> = _liveStats.asStateFlow()

    private val _sensorMode = MutableStateFlow(SensorMode.LINEAR_ACCELERATION)
    val sensorMode: StateFlow<SensorMode> = _sensorMode.asStateFlow()

    // Target sampling delay in microseconds (default SENSOR_DELAY_GAME = 20,000us ~ 50Hz)
    private val _samplingDelayUs = MutableStateFlow(SensorManager.SENSOR_DELAY_GAME)
    val samplingDelayUs: StateFlow<Int> = _samplingDelayUs.asStateFlow()

    // Tare offsets
    private var tareX = 0f
    private var tareY = 0f
    private var tareZ = 0f
    private val _isTared = MutableStateFlow(false)
    val isTared: StateFlow<Boolean> = _isTared.asStateFlow()

    // High-pass filter variables for raw accelerometer fallback
    private val gravity = FloatArray(3)
    private val alpha = 0.8f // HP filter constant

    // Session Recording variables
    private var currentSessionId: Long = 0L
    private var sessionStartTime: Long = 0L
    private var sessionTitle: String = "Vibration Log"
    private var lastRecordedStats = LiveSessionStats()
    private var sumMagnitudes = 0.0
    private var sumSquares = 0.0
    private var recordedCount = 0
    private var minMag = Float.MAX_VALUE
    private var maxMag = 0f

    // In-memory buffer to batch write to Room database
    private val pendingSamples = mutableListOf<VibrationSampleEntity>()
    private val batchLock = Any()
    private val rollingList = ArrayDeque<VibrationReading>(300)
    private var lastBufferEmitTime = 0L

    init {
        registerSensorListener()
    }

    fun setSensorMode(mode: SensorMode) {
        if (_sensorMode.value != mode) {
            _sensorMode.value = mode
            resetTare()
            registerSensorListener()
        }
    }

    fun setSamplingRate(delayUs: Int) {
        if (_samplingDelayUs.value != delayUs) {
            _samplingDelayUs.value = delayUs
            registerSensorListener()
        }
    }

    fun tare() {
        val current = _currentReading.value
        tareX += current.x
        tareY += current.y
        tareZ += current.z
        _isTared.value = true
    }

    fun resetTare() {
        tareX = 0f
        tareY = 0f
        tareZ = 0f
        _isTared.value = false
    }

    private fun registerSensorListener() {
        sensorManager.unregisterListener(this)

        val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        activeSensor = when (_sensorMode.value) {
            SensorMode.LINEAR_ACCELERATION -> linearSensor ?: accelSensor
            SensorMode.RAW_ACCELEROMETER -> accelSensor ?: linearSensor
        }

        activeSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, _samplingDelayUs.value)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawX = event.values[0]
        var rawY = event.values[1]
        var rawZ = event.values[2]

        // If raw accelerometer is used, apply High-Pass filter to strip gravity baseline
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && _sensorMode.value == SensorMode.RAW_ACCELEROMETER) {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * rawX
            gravity[1] = alpha * gravity[1] + (1 - alpha) * rawY
            gravity[2] = alpha * gravity[2] + (1 - alpha) * rawZ

            rawX -= gravity[0]
            rawY -= gravity[1]
            rawZ -= gravity[2]
        }

        // Apply Tare calibration offset
        val x = rawX - tareX
        val y = rawY - tareY
        val z = rawZ - tareZ
        val mag = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()
        val elapsed = if (_isRecording.value) now - sessionStartTime else 0L

        val reading = VibrationReading(
            timestamp = now,
            elapsedMs = elapsed,
            x = x,
            y = y,
            z = z,
            magnitude = mag
        )

        _currentReading.value = reading

        // Update rolling buffer for UI rendering (throttled to ~60fps)
        val nowClock = SystemClock.uptimeMillis()
        synchronized(rollingList) {
            if (rollingList.size >= 250) {
                rollingList.removeFirst()
            }
            rollingList.addLast(reading)
        }

        if (nowClock - lastBufferEmitTime >= 20) { // ~50fps emit
            lastBufferEmitTime = nowClock
            synchronized(rollingList) {
                _liveBuffer.value = rollingList.toList()
            }
        }

        // Handle recording accumulation
        if (_isRecording.value) {
            recordedCount++
            sumMagnitudes += mag
            sumSquares += (mag * mag)
            minMag = min(minMag, mag)
            maxMag = max(maxMag, mag)

            val avg = (sumMagnitudes / recordedCount).toFloat()
            val rms = sqrt(sumSquares / recordedCount).toFloat()

            val stats = LiveSessionStats(
                elapsedMs = elapsed,
                sampleCount = recordedCount,
                minIntensity = if (minMag == Float.MAX_VALUE) 0f else minMag,
                maxIntensity = maxMag,
                avgIntensity = avg,
                rmsIntensity = rms,
                currentIntensity = mag
            )
            _liveStats.value = stats
            lastRecordedStats = stats

            val sample = VibrationSampleEntity(
                sessionId = currentSessionId,
                timestamp = now,
                elapsedMs = elapsed,
                x = x,
                y = y,
                z = z,
                magnitude = mag
            )

            var shouldFlush = false
            var toFlush: List<VibrationSampleEntity>? = null
            synchronized(batchLock) {
                pendingSamples.add(sample)
                if (pendingSamples.size >= 100) {
                    toFlush = ArrayList(pendingSamples)
                    pendingSamples.clear()
                    shouldFlush = true
                }
            }

            if (shouldFlush && toFlush != null) {
                val batch = toFlush!!
                scope.launch(Dispatchers.IO) {
                    dao.insertSamples(batch)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Starts background recording service and begins persisting vibration samples.
     */
    fun startRecording(context: Context, title: String = "Vibration Log") {
        if (_isRecording.value) return

        sessionStartTime = System.currentTimeMillis()
        sessionTitle = title.ifBlank { "Vibration Log" }
        recordedCount = 0
        sumMagnitudes = 0.0
        sumSquares = 0.0
        minMag = Float.MAX_VALUE
        maxMag = 0f
        lastRecordedStats = LiveSessionStats()
        _liveStats.value = lastRecordedStats

        synchronized(batchLock) {
            pendingSamples.clear()
        }

        // Create initial session in DB
        scope.launch(Dispatchers.IO) {
            val sampleRateHz = when (_samplingDelayUs.value) {
                SensorManager.SENSOR_DELAY_FASTEST -> 100
                SensorManager.SENSOR_DELAY_GAME -> 50
                SensorManager.SENSOR_DELAY_UI -> 20
                else -> 50
            }

            val initialSession = VibrationSessionEntity(
                title = sessionTitle,
                startTime = sessionStartTime,
                endTime = sessionStartTime,
                durationMs = 0L,
                sampleCount = 0,
                minIntensity = 0f,
                maxIntensity = 0f,
                avgIntensity = 0f,
                rmsIntensity = 0f,
                sensorType = _sensorMode.value.displayName,
                samplingRateHz = sampleRateHz,
                notes = ""
            )

            currentSessionId = dao.insertSession(initialSession)
            _isRecording.value = true

            // Start Android Foreground Service for continuous background capture
            val serviceIntent = Intent(context, VibrationRecordingService::class.java).apply {
                action = VibrationRecordingService.ACTION_START
                putExtra(VibrationRecordingService.EXTRA_SESSION_ID, currentSessionId)
                putExtra(VibrationRecordingService.EXTRA_TITLE, sessionTitle)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    /**
     * Stops the background recording, flushes any remaining samples, updates session stats,
     * and stops the service. Returns the saved session ID via callback.
     */
    fun stopRecording(context: Context, onSaved: (Long) -> Unit = {}) {
        if (!_isRecording.value) return
        _isRecording.value = false

        // Stop foreground service
        val serviceIntent = Intent(context, VibrationRecordingService::class.java).apply {
            action = VibrationRecordingService.ACTION_STOP
        }
        context.startService(serviceIntent)

        val end = System.currentTimeMillis()
        val duration = end - sessionStartTime
        val sid = currentSessionId
        val stats = lastRecordedStats

        scope.launch(Dispatchers.IO) {
            // Flush remaining pending samples
            var remaining: List<VibrationSampleEntity>? = null
            synchronized(batchLock) {
                if (pendingSamples.isNotEmpty()) {
                    remaining = ArrayList(pendingSamples)
                    pendingSamples.clear()
                }
            }
            remaining?.let { dao.insertSamples(it) }

            val totalCount = dao.getSampleCountForSession(sid)

            val updatedSession = VibrationSessionEntity(
                id = sid,
                title = sessionTitle,
                startTime = sessionStartTime,
                endTime = end,
                durationMs = duration,
                sampleCount = totalCount,
                minIntensity = stats.minIntensity,
                maxIntensity = stats.maxIntensity,
                avgIntensity = stats.avgIntensity,
                rmsIntensity = stats.rmsIntensity,
                sensorType = _sensorMode.value.displayName,
                samplingRateHz = if (duration > 0) ((totalCount * 1000L) / duration).toInt() else 50,
                notes = ""
            )

            dao.updateSession(updatedSession)

            launch(Dispatchers.Main) {
                onSaved(sid)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: VibrationRecorderManager? = null

        fun getInstance(context: Context): VibrationRecorderManager {
            return INSTANCE ?: synchronized(this) {
                val instance = VibrationRecorderManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
