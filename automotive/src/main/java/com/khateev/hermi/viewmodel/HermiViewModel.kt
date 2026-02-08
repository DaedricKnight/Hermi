package com.khateev.hermi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khateev.hermi.repository.VhalReader
import com.khateev.hermi.shared.HermiAIProcessor
import com.khateev.hermi.shared.SpeechToTextManager
import com.khateev.hermi.shared.VhalParamsState
import com.khateev.hermi.shared.VoiceInterfaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class HermiViewModel (
    private val speechToTextManager: SpeechToTextManager,
    private val vhalReader: VhalReader,
    private val aiProcessor: HermiAIProcessor,
    private val voiceManager: VoiceInterfaceManager
) : ViewModel() {

    val uiState: StateFlow<VhalParamsState> = vhalReader.carState

    private val _isMockMode = MutableStateFlow(true)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    val isListening: StateFlow<Boolean> = speechToTextManager.listeningState
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), false)

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>()
    val recognizedText = _recognizedText.asSharedFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            uiState.collectLatest { state ->
                aiProcessor.processVehicleData(state).collect { aiMessage ->
                    voiceManager.speak(aiMessage)
                    _events.emit(aiMessage)
                }
            }
        }

        viewModelScope.launch {
            speechToTextManager.spokenText.collect { text ->
                _currentSpokenText.value = text
                _recognizedText.emit(text)

                val command = aiProcessor.parseCommand(text)
                executeCommand(command)
            }
        }
    }

    private fun executeCommand(command: HermiAIProcessor.Command) {
        when (command) {
            is HermiAIProcessor.HermiCommand.SwitchToReal -> {
                toggleVhalSource(useMock = false)
                voiceManager.speak("Switching to real vehicle data")
            }
            is HermiAIProcessor.HermiCommand.SwitchToMock -> {
                toggleVhalSource(useMock = true)
                voiceManager.speak("Simulation mode activated")
            }
            is HermiAIProcessor.HermiCommand.GetVehicleStatus -> {
                val status = if (uiState.value.isCheckEngineOn) {
                    "Alert! Check engine is on. Please scan for DTC codes."
                } else {
                    "Vehicle systems are normal. Fuel level is ${uiState.value.fuelLevel} milliliters."
                }
                voiceManager.speak(status)
            }

            else -> {
                voiceManager.speak("I didn't quite get that command")
            }
        }
    }

    fun toggleVhalSource(useMock: Boolean) {
        viewModelScope.launch {
//            _isMockMode.value = useMock
//            (vhalReader as? HybridVhalReader)?.toggleMode(useMock)
//            val msg = if (useMock) "Simulation Mode Active" else "Real VHAL Connected"
//            _events.emit(msg)
        }
    }

    fun startVoiceInput() {
        Log.d("HermiVM", "startVoiceInput called")
        speechToTextManager.startListening()
    }

    fun stopVoiceInput() = speechToTextManager.stopListening()

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
        speechToTextManager.destroy()
    }
}