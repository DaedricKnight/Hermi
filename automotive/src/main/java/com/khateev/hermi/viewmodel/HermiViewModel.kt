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
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
import com.khateev.hermi.shared.model.Command
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

class HermiViewModel(
    private val voskManager: RecognitionManager,
    private val audioRecorderImpl: AudioRecorder,
    private val vhalReader: VhalReader,
    private val aiProcessor: AIProcessor,
    private val voiceManager: VoiceInterfaceManager,
) : ViewModel() {

    val uiState: StateFlow<VhalParamsState> = vhalReader.carState

    var _isRecording = MutableStateFlow(false)

    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var transcription by mutableStateOf("")
        private set

    var isInitialized by mutableStateOf(false)
        private set

    var _isProcessing = MutableStateFlow(false)

    val isProcessing = _isProcessing.asStateFlow()

    var _hasError = MutableStateFlow<Exception?>(null)

    val hasError = _hasError.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>(replay = 1)
    val recognizedText = _recognizedText.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val TAG = "HermiVM"

    init {
        viewModelScope.launch {
            try {
                transcription = "Loading Vosk model..."
                log("Calling voskManager.initialize()...")

                delay(100)

                isInitialized = voskManager.initialize()

                if (isInitialized) {
                    log("VoskManager initialized successfully!")
                    transcription = "🎤 Ready to record"
                } else {
                    Log.e(TAG, "VoskManager initialization FAILED")
                    transcription = "Initialization failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during initialization", e)
                transcription = "Error: ${e.message}"
            }
        }
    }

    fun startRecording() {
        if (!isInitialized) {
            Log.e("HermiVM", "Cannot record: Vosk not initialized")
            transcription = "Not initialized yet"
            return
        }

        viewModelScope.launch {
            _isRecording.value = true
            transcription = "Recording..."
            audioRecorderImpl.startRecording()
        }
    }

    fun stopRecordingAndTranscribe() {

        if (!isInitialized) {
            Log.e("HermiVM", "Cannot transcribe: Vosk not initialized")
            return
        }

        _isRecording.value = false
        val samples = audioRecorderImpl.stopRecording()

        log("Got ${samples.size} samples, starting transcription...")

        viewModelScope.launch {
            try {
                _isProcessing.value = true
                transcription = "Transcribing..."

                val result = voskManager.recognizeAudio(samples)

                if (result != null) {
                    log("Transcription: $result")
                    transcription = result
                    val command = aiProcessor.parseCommand(result)
                    executeCommand(command)
                    delay(5000)
                    _recognizedText.emit("")
                } else {
                    log("Transcription returned null")
                    transcription = "Failed to transcribe"
                }
            } catch (e: Exception) {
                Log.e("HermiVM", "Transcription error", e)
                transcription = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun executeCommand(command: Command) {
        log("executeCommand: $command")

        viewModelScope.launch {
            when (command) {
                // Navigation commands
                is Command.FindFood -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("restaurants"))
                    voiceManager.speak("Searching for nearby restaurants and cafes")
                }

                is Command.FindGasStation -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("gas stations"))
                    voiceManager.speak("Finding gas stations near you")
                }

                is Command.FindChargingStation -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("EV charging stations"))
                    voiceManager.speak("Locating charging stations nearby")
                }

                is Command.FindParking -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("parking"))
                    voiceManager.speak("Searching for parking areas")
                }

                // Vehicle status commands
                is Command.VehicleStatus -> {
                    val status = buildVehicleStatusReport()
                    voiceManager.speak(status)
                }

                is Command.CheckEngine -> {
                    val engineStatus = buildEngineStatusReport()
                    voiceManager.speak(engineStatus)
                }

                is Command.CheckTires -> {
                    val tireStatus = buildTireStatusReport()
                    voiceManager.speak(tireStatus)
                }

                is Command.CheckFuelLevel -> {
                    val fuelStatus = buildFuelStatusReport()
                    voiceManager.speak(fuelStatus)
                }

                is Command.CheckBatteryLevel -> {
                    val batteryStatus = buildBatteryStatusReport()
                    voiceManager.speak(batteryStatus)
                }

                // Control commands
                is Command.LockDoors -> {
                    sendVehicleCommand(VehicleCommand.LOCK_DOORS)
                    voiceManager.speak("Locking all doors")
                }

                is Command.UnlockDoors -> {
                    sendVehicleCommand(VehicleCommand.UNLOCK_DOORS)
                    voiceManager.speak("Unlocking all doors")
                }

                is Command.TurnOnLights -> {
                    sendVehicleCommand(VehicleCommand.LIGHTS_ON)
                    voiceManager.speak("Turning on headlights")
                }

                is Command.TurnOffLights -> {
                    sendVehicleCommand(VehicleCommand.LIGHTS_OFF)
                    voiceManager.speak("Turning off headlights")
                }

                // Information commands
                is Command.GetRange -> {
                    val range = uiState.value.range
                    voiceManager.speak("Your current range is ${range.toInt()} kilometers")
                }

                is Command.GetOdometer -> {
                    val odometer = uiState.value.odometer
                    voiceManager.speak("Total mileage is ${odometer.toInt()} kilometers")
                }

                is Command.GetSpeed -> {
                    val speed = uiState.value.speed
                    voiceManager.speak("Current speed is ${speed.toInt()} kilometers per hour")
                }

                is Command.GetOutsideTemp -> {
                    val temp = uiState.value.outsideTemp
                    val unit = uiState.value.tempUnit.ifEmpty { "°Celcium" }
                    voiceManager.speak("Outside temperature is ${temp.toInt()} $unit")
                }

                is Command.Unknown -> {
                    voiceManager.speak("Unknown command")
                }
            }
        }
    }

    // Helper functions to build status reports
    private fun buildVehicleStatusReport(): String {
        val state = uiState.value
        return buildString {
            if (state.isCheckEngineOn) {
                append("Warning! Check engine light is on. ")
            }
            if (state.isCriticallyLowTirePressure) {
                append("Critical tire pressure detected. ")
            }
            if (state.isFuelLow) {
                append("Fuel level is low. ")
            }
            if (isEmpty()) {
                append("All vehicle systems are operating normally. ")
            }
            append("Fuel level is ${state.fuelLevel.toInt()} percent. ")
            append("Range is ${state.range.toInt()} kilometers.")
        }
    }

    private fun buildEngineStatusReport(): String {
        val state = uiState.value
        return if (state.isCheckEngineOn) {
            val dtcInfo = if (state.activeDTCs.isNotEmpty()) {
                "Active diagnostic codes: ${state.activeDTCs.joinToString(", ")}"
            } else {
                "Please perform a diagnostic scan"
            }
            "Check engine light is on. $dtcInfo"
        } else {
            "Engine is running normally. " +
                    "Coolant temperature is ${state.coolantTemp.toInt()} degrees. " +
                    "Oil level is ${state.oilLevel.toInt()} percent."
        }
    }

    private fun buildTireStatusReport(): String {
        val state = uiState.value
        return if (state.isCriticallyLowTirePressure) {
            val lowTires = state.tirePressures.filter { it.value < 2.0f }
            "Warning! Low tire pressure detected in ${lowTires.size} tire(s). Please check immediately."
        } else if (state.tirePressures.isNotEmpty()) {
            "All tire pressures are within normal range"
        } else {
            "Tire pressure data is not available"
        }
    }

    private fun buildFuelStatusReport(): String {
        val state = uiState.value
        return buildString {
            append("Fuel level is ${state.fuelLevel.toInt()} percent. ")
            if (state.isFuelLow) {
                append("Warning: fuel is low. ")
            }
            append("Range is ${state.range.toInt()} kilometers.")
        }
    }

    private fun buildBatteryStatusReport(): String {
        val state = uiState.value
        return if (state.evBatteryLevel > 0) {
            buildString {
                append("Battery charge is ${state.evBatteryLevel.toInt()} percent. ")
                if (state.isEvChargePortConnected) {
                    append("Charging port is connected. ")
                    if (state.evChargeTime > 0) {
                        append("Estimated time to full charge: ${state.evChargeTime} minutes.")
                    }
                } else if (state.evBatteryLevel < 20) {
                    append("Warning: battery is low. Please charge soon.")
                }
            }
        } else {
            "This vehicle does not have an electric battery"
        }
    }

    enum class VehicleCommand {
        LOCK_DOORS,
        UNLOCK_DOORS,
        LIGHTS_ON,
        LIGHTS_OFF
    }

    // Function to send commands to vehicle HAL
    private fun sendVehicleCommand(command: VehicleCommand) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual vehicle HAL command sending
                // vhalManager.sendCommand(command)
                log("Sending vehicle command: $command")
            } catch (e: Exception) {
                Log.e("HermiVM", "Failed to send vehicle command: ${e.message}")
                _hasError.value = Exception("Failed to send vehicle command: ${e.message}")
                voiceManager.speak("Failed to execute command")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
        voskManager.release()
    }
}

private fun log(message: String) = Log.d("HermiViewModel", message)