package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.VibrationSampleEntity
import com.example.data.local.VibrationSessionEntity
import com.example.model.VibrationLevel
import com.example.model.VibrationReading
import com.example.model.VibrationUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read app_name string from context matches VibroLog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VibroLog", appName)
    }

    @Test
    fun `vibration magnitude calculation is accurate`() {
        val mag = VibrationReading.calculateMagnitude(3.0f, 4.0f, 0.0f)
        assertEquals(5.0f, mag, 0.001f)

        val mag3D = VibrationReading.calculateMagnitude(2.0f, 3.0f, 6.0f)
        assertEquals(7.0f, mag3D, 0.001f)
    }

    @Test
    fun `vibration level categorization behaves correctly`() {
        assertEquals(VibrationLevel.CALM, VibrationLevel.fromMagnitude(0.1f))
        assertEquals(VibrationLevel.LIGHT, VibrationLevel.fromMagnitude(0.5f))
        assertEquals(VibrationLevel.MODERATE, VibrationLevel.fromMagnitude(1.8f))
        assertEquals(VibrationLevel.STRONG, VibrationLevel.fromMagnitude(3.5f))
        assertEquals(VibrationLevel.SEVERE, VibrationLevel.fromMagnitude(7.2f))
    }

    @Test
    fun `vibration unit formatting produces expected units`() {
        val valMs2 = 9.80665f
        val gFormatted = VibrationUnit.G_FORCE.format(valMs2)
        assertTrue(gFormatted.contains("1.000 g"))

        val mmFormatted = VibrationUnit.MILLIMETERS_PER_SEC_SQ.format(2.5f)
        assertTrue(mmFormatted.contains("2500 mm/s²"))
    }

    @Test
    fun `session insert and sample queries work in Room database`() = runBlocking {
        val dao = db.vibrationDao()
        val now = System.currentTimeMillis()

        val session = VibrationSessionEntity(
            title = "Engine Test",
            startTime = now,
            endTime = now + 5000,
            durationMs = 5000,
            sampleCount = 3,
            minIntensity = 0.5f,
            maxIntensity = 4.2f,
            avgIntensity = 2.1f,
            rmsIntensity = 2.5f,
            sensorType = "Linear Acceleration",
            samplingRateHz = 50,
            notes = "Test run on vehicle dashboard"
        )

        val sessionId = dao.insertSession(session)
        assertTrue(sessionId > 0)

        val retrieved = dao.getSessionById(sessionId)
        assertNotNull(retrieved)
        assertEquals("Engine Test", retrieved?.title)
        assertEquals(4.2f, retrieved?.maxIntensity ?: 0f, 0.001f)

        // Insert samples
        val samples = listOf(
            VibrationSampleEntity(sessionId = sessionId, timestamp = now, elapsedMs = 0, x = 0.1f, y = 0.2f, z = 0.3f, magnitude = 0.5f),
            VibrationSampleEntity(sessionId = sessionId, timestamp = now + 100, elapsedMs = 100, x = 1.0f, y = 2.0f, z = 3.0f, magnitude = 3.74f),
            VibrationSampleEntity(sessionId = sessionId, timestamp = now + 200, elapsedMs = 200, x = 2.0f, y = 3.0f, z = 2.0f, magnitude = 4.2f)
        )
        dao.insertSamples(samples)

        val retrievedSamples = dao.getSamplesForSession(sessionId)
        assertEquals(3, retrievedSamples.size)
        assertEquals(4.2f, retrievedSamples.last().magnitude, 0.001f)
    }
}
