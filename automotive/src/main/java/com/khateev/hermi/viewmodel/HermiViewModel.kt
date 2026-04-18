package com.khateev.hermi.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khateev.hermi.NavigationEvent
import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.interfaces.AudioRecorder
import com.khateev.hermi.shared.interfaces.RecognitionManager
import com.khateev.hermi.shared.interfaces.VersionManager
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandValidator
import com.khateev.hermi.shared.model.CommandAccessResult
import com.khateev.hermi.shared.ai.VADManager
import com.qubit.core_vhal.VhalParamsState
import com.qubit.core_vhal.repository.VhalReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class HermiViewModel(
    private val voskManager: RecognitionManager,
    private val audioRecorderImpl: AudioRecorder,
    private val vhalReader: VhalReader,
    private val aiProcessor: AIProcessor,
    private val voiceManager: VoiceInterfaceManager,
    private val versionManager: VersionManager,
    private val vadManager: VADManager,
) : ViewModel() {

    val uiState: StateFlow<VhalParamsState> = vhalReader.carState

    private var _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var transcription by mutableStateOf("")
        private set

    var isInitialized by mutableStateOf(false)
        private set

    private var _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private var _hasError = MutableStateFlow<Exception?>(null)
    val hasError = _hasError.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>(replay = 1)
    val recognizedText = _recognizedText.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private var vadJob: Job? = null
    private var fallbackJob: Job? = null
    private val TAG = "HermiVM"

    init {
        viewModelScope.launch {
            try {
                transcription = "Loading Vosk model..."
                Log.d(TAG, "Initializing VoskManager...")
                delay(100)
                isInitialized = voskManager.initialize()

                if (isInitialized) {
                    Log.d(TAG, "VoskManager initialized successfully")
                    transcription = "Ready to record"
                } else {
                    Log.e(TAG, "VoskManager initialization FAILED")
                    transcription = "Initialization failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during initialization: ${e.message}", e)
                transcription = "Error: ${e.message}"
            }
        }
    }

    fun startRecording() {
        if (!isInitialized) {
            Log.e(TAG, "Cannot start recording: Vosk is not initialized")
            transcription = "Not initialized yet"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isRecording.value = true
            transcription = "Listening..."
            Log.d(TAG, "Manual recording start requested")
            
            // Fallback: stop after 7s if VAD fails to trigger
            fallbackJob?.cancel()
            fallbackJob = launch { 
                delay(7000) 
                if (_isRecording.value) {
                    Log.d(TAG, "Fallback timeout reached (7s). Stopping recording.")
                    stopRecordingAndTranscribe()
                }
            }
            
            startVadMonitoring()
            audioRecorderImpl.startRecording()
        }
    }

    private fun startVadMonitoring() {
        vadJob?.cancel()
        vadJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "Starting VAD speech monitoring")
                vadManager.monitorSpeech(audioRecorderImpl.audioFlow) {
                    Log.d(TAG, "VAD: Speech end detected. Processing.")
                    stopRecordingAndTranscribe()
                }
            } catch (e: Exception) {
                Log.e(TAG, "VAD monitoring error: ${e.message}", e)
            }
        }
    }

    fun stopRecordingAndTranscribe() {
        if (!isInitialized || !_isRecording.value) return 

        Log.d(TAG, "stopRecordingAndTranscribe() called")
        _isRecording.value = false
        vadJob?.cancel()
        fallbackJob?.cancel()
        
        val samples = audioRecorderImpl.stopRecording()
        Log.d(TAG, "Recording stopped. Samples captured: ${samples.size}")

        viewModelScope.launch {
            try {
                _isProcessing.value = true
                transcription = "Processing..."

                // Use the full samples array for stable recognition
                val result = voskManager.recognizeAudio(samples)

                if (!result.isNullOrBlank()) {
                    Log.d(TAG, "Transcription result: \"$result\"")
                    transcription = result
                    _recognizedText.emit(result)
                    
                    val command = aiProcessor.parseCommand(result)
                    executeCommand(command)
                } else {
                    Log.d(TAG, "Transcription returned empty result")
                    transcription = "I didn't catch that"
                    delay(1500)
                    _navigationEvent.emit(NavigationEvent.CloseAssistant)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}", e)
                _navigationEvent.emit(NavigationEvent.CloseAssistant)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun executeCommand(command: Command) {
        Log.d(TAG, "Executing command: $command")
        viewModelScope.launch {
            val isPro = versionManager.isProNow()
            val accessResult = CommandValidator.checkCommandAccess(command, isPro)

            if (accessResult == CommandAccessResult.RequiresPro) {
                voiceManager.speak("This command requires a Pro subscription.")
                delay(2000)
                _navigationEvent.emit(NavigationEvent.CloseAssistant)
                return@launch
            }

            if (accessResult == CommandAccessResult.Unavailable) {
                voiceManager.speak("This command is currently unavailable.")
                delay(2000)
                _navigationEvent.emit(NavigationEvent.CloseAssistant)
                return@launch
            }

            when (command) {
                is Command.FindFood -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("restaurants"))
                    voiceManager.speak("Searching for nearby restaurants.")
                }
                is Command.FindGasStation -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("gas stations"))
                    voiceManager.speak("Locating the nearest gas stations.")
                }
                is Command.FindChargingStation -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("EV charging stations"))
                    voiceManager.speak("Finding EV charging stations nearby.")
                }
                is Command.FindParking -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("parking"))
                    voiceManager.speak("Searching for available parking areas.")
                }
                is Command.FindRestStop -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("rest stops"))
                    voiceManager.speak("Finding rest stops on your route.")
                }
                is Command.CheckEngineStatus -> voiceManager.speak(buildEngineStatusReport())
                is Command.CheckOilLevel -> {
                    val state = uiState.value
                    voiceManager.speak("Oil level is ${state.oilLevel.toInt()} percent.")
                }
                is Command.CheckTransmissionStatus -> {
                    val state = uiState.value
                    voiceManager.speak("Transmission gear: ${state.gear}. Engine RPM: ${state.rpm}.")
                }
                is Command.CheckRpm -> voiceManager.speak("Engine RPM: ${uiState.value.rpm}.")
                is Command.CheckFuelLevel -> voiceManager.speak(buildFuelStatusReport())
                is Command.CheckBatteryVoltage -> {
                    val state = uiState.value
                    voiceManager.speak("Battery level is ${state.evBatteryLevel.toInt()} percent.")
                }
                is Command.CheckEvStatus -> {
                    val state = uiState.value
                    voiceManager.speak("EV battery at ${state.evBatteryLevel.toInt()} percent.")
                }
                is Command.CheckTireStatus -> voiceManager.speak(buildTireStatusReport())
                is Command.CheckOdometer -> voiceManager.speak("Total distance: ${uiState.value.odometer.toInt()} km.")
                is Command.CheckDrivingRange -> voiceManager.speak("Range: ${uiState.value.range.toInt()} km.")
                is Command.CheckOutsideTemperature -> {
                    val temp = uiState.value.outsideTemp.toInt()
                    voiceManager.speak("Temperature is $temp degrees.")
                }
                is Command.FindService -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("auto repair"))
                    voiceManager.speak("Searching for auto repair services.")
                }
                is Command.FindCarWash -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("car wash"))
                    voiceManager.speak("Searching for car wash stations.")
                }
                is Command.Unknown -> {
                    voiceManager.speak("I'm sorry, I don't know how to do that.")
                }
                else -> {}
            }
            
            // Close immediately after starting speech/action
            _navigationEvent.emit(NavigationEvent.CloseAssistant)
        }
    }

    private fun buildEngineStatusReport(): String {
        val state = uiState.value
        return if (state.isCheckEngineOn) {
            "The check engine light is active. Please visit a service station."
        } else {
            "The engine system is functioning normally."
        }
    }

    private fun buildTireStatusReport(): String {
        val state = uiState.value
        return if (state.isCriticallyLowTirePressure) {
            "Warning: Low tire pressure detected. Check your tires immediately."
        } else {
            "Tire pressure is within the normal range."
        }
    }

    private fun buildFuelStatusReport(): String {
        val state = uiState.value
        return "Fuel level is ${state.fuelLevel.toInt()} percent. Estimated range is ${state.range.toInt()} kilometers."
    }

    override fun onCleared() {
        super.onCleared()
        vadJob?.cancel()
        fallbackJob?.cancel()
        voiceManager.shutdown()
        voskManager.release()
        vadManager.close()
    }
}
