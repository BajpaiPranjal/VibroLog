package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VibrationSampleEntity
import com.example.data.local.VibrationSessionEntity
import com.example.data.repository.VibrationRepository
import com.example.model.SensorMode
import com.example.model.VibrationReading
import com.example.model.VibrationUnit
import com.example.service.LiveSessionStats
import com.example.service.VibrationRecorderManager
import com.example.ui.components.GraphDisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareCsv(val intent: Intent) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VibrationRepository.getInstance(application)
    private val recorderManager = VibrationRecorderManager.getInstance(application)

    // Recorder state forwarded
    val isRecording: StateFlow<Boolean> = recorderManager.isRecording
    val currentReading: StateFlow<VibrationReading> = recorderManager.currentReading
    val liveBuffer: StateFlow<List<VibrationReading>> = recorderManager.liveBuffer
    val liveStats: StateFlow<LiveSessionStats> = recorderManager.liveStats
    val sensorMode: StateFlow<SensorMode> = recorderManager.sensorMode
    val samplingDelayUs: StateFlow<Int> = recorderManager.samplingDelayUs
    val isTared: StateFlow<Boolean> = recorderManager.isTared

    // DB Sessions
    val sessions: StateFlow<List<VibrationSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Tab (0 = Monitor, 1 = History, 2 = Settings)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Config options
    private val _vibrationUnit = MutableStateFlow(VibrationUnit.METERS_PER_SEC_SQ)
    val vibrationUnit: StateFlow<VibrationUnit> = _vibrationUnit.asStateFlow()

    private val _graphDisplayMode = MutableStateFlow(GraphDisplayMode.MAGNITUDE)
    val graphDisplayMode: StateFlow<GraphDisplayMode> = _graphDisplayMode.asStateFlow()

    // Session Detail / Inspection State
    private val _selectedSession = MutableStateFlow<VibrationSessionEntity?>(null)
    val selectedSession: StateFlow<VibrationSessionEntity?> = _selectedSession.asStateFlow()

    private val _selectedSessionSamples = MutableStateFlow<List<VibrationSampleEntity>>(emptyList())
    val selectedSessionSamples: StateFlow<List<VibrationSampleEntity>> = _selectedSessionSamples.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    // Events (e.g. Export sharing)
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setVibrationUnit(unit: VibrationUnit) {
        _vibrationUnit.value = unit
    }

    fun setGraphDisplayMode(mode: GraphDisplayMode) {
        _graphDisplayMode.value = mode
    }

    fun setSensorMode(mode: SensorMode) {
        recorderManager.setSensorMode(mode)
    }

    fun setSamplingRate(delayUs: Int) {
        recorderManager.setSamplingRate(delayUs)
    }

    fun tare() {
        recorderManager.tare()
    }

    fun resetTare() {
        recorderManager.resetTare()
    }

    fun startRecording(title: String) {
        val app = getApplication<Application>()
        recorderManager.startRecording(app, title)
    }

    fun stopRecording() {
        val app = getApplication<Application>()
        recorderManager.stopRecording(app) { savedId ->
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Session recorded & saved successfully!"))
                // Optionally open the newly saved session
                selectSession(savedId)
            }
        }
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val session = repository.getSessionById(sessionId)
            _selectedSession.value = session
            if (session != null) {
                val samples = repository.getSamplesForSession(sessionId)
                _selectedSessionSamples.value = samples
            } else {
                _selectedSessionSamples.value = emptyList()
            }
            _isLoadingDetail.value = false
        }
    }

    fun clearSelectedSession() {
        _selectedSession.value = null
        _selectedSessionSamples.value = emptyList()
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSession.value?.id == sessionId) {
                clearSelectedSession()
            }
            _uiEvents.emit(UiEvent.ShowToast("Session deleted"))
        }
    }

    fun exportSessionCsv(sessionId: Long) {
        viewModelScope.launch {
            val result = repository.exportSessionToCsv(sessionId)
            when (result) {
                is VibrationRepository.ExportResult.Success -> {
                    val shareIntent = repository.createShareIntent(result.uri, result.fileName)
                    val chooser = Intent.createChooser(shareIntent, "Export Vibration CSV (${result.fileName})")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    _uiEvents.emit(UiEvent.ShareCsv(chooser))
                }
                is VibrationRepository.ExportResult.Error -> {
                    _uiEvents.emit(UiEvent.ShowToast("Export failed: ${result.message}"))
                }
            }
        }
    }
}
