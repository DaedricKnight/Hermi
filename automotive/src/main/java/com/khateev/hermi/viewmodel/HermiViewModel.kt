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
    private val versionManager: VersionManager,
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
            // Check if command is available for current version
            val isPro = versionManager.isProNow()
            val accessResult = CommandValidator.checkCommandAccess(command, isPro)

            // Handle version access restrictions
            if (accessResult == CommandAccessResult.RequiresPro) {
                voiceManager.speak(
                    "This command is not included in the free package. " +
                    "You can upgrade to Pro subscription to access this feature"
                )
                log("Pro command attempted by free user: $command")
                return@launch
            }

            if (accessResult == CommandAccessResult.Unavailable) {
                voiceManager.speak("This command is not available at the moment")
                log("Unavailable command attempted: $command")
                return@launch
            }

            when (command) {

                // ==================== FREE VERSION ====================

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
                    voiceManager.speak("Locating EV charging stations nearby")
                }

                is Command.FindParking -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("parking"))
                    voiceManager.speak("Searching for parking areas")
                }

                is Command.FindRestStop -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("rest stops"))
                    voiceManager.speak("Finding rest areas nearby")
                }

                // ==================== PRO: ENGINE & POWERTRAIN ====================

                is Command.CheckEngineStatus -> {
                    // VHAL fields: isCheckEngineOn, activeDTCs, coolantTemp
                    voiceManager.speak(buildEngineStatusReport())
                }

                is Command.CheckOilLevel -> {
                    // VHAL fields: oilLevel (Float), oilTemp (Float)
                    val state = uiState.value
                    val oilLevelPct = state.oilLevel.toInt()
                    val oilTemp = state.oilTemp.toInt()
                    voiceManager.speak("Oil level is at $oilLevelPct percent. Oil temperature is $oilTemp degrees")
                }

                is Command.CheckTransmissionStatus -> {
                    // No direct VHAL field — using gear + rpm as proxy
                    val state = uiState.value
                    val gear = state.gear
                    val rpm = state.rpm
                    voiceManager.speak("Current gear is $gear, engine RPM is $rpm")
                }

                is Command.CheckRpm -> {
                    // VHAL field: rpm (Int) — NEW COMMAND
                    val rpm = uiState.value.rpm
                    voiceManager.speak("Engine RPM is $rpm")
                }

                is Command.CheckBrakePadWear -> {
                    // No VHAL field — OBD2 only. isAbsActive used as context
                    val absStatus = if (uiState.value.isAbsActive) "ABS is currently active" else "ABS is on standby"
                    voiceManager.speak("Brake pad wear data requires OBD2 connection. $absStatus")
                }

                is Command.CheckSparkPlugCondition -> {
                    // No VHAL field — OBD2 only. activeDTCs used as context
                    val hasDtc = uiState.value.activeDTCs.isNotEmpty()
                    val dtcHint = if (hasDtc) "Active fault codes detected. Check engine diagnostics." else "No ignition-related fault codes detected"
                    voiceManager.speak("Spark plug data requires OBD2 connection. $dtcHint")
                }

                // ==================== PRO: FUEL & BATTERY ====================

                is Command.CheckFuelLevel -> {
                    // VHAL fields: fuelLevel, isFuelLow, fuelCapacity, fuelCapacityType
                    voiceManager.speak(buildFuelStatusReport())
                }

                is Command.CheckFuelConsumption -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Fuel consumption data requires OBD2 connection")
                }

                is Command.CheckBatteryVoltage -> {
                    // No direct batteryVoltage in VHAL — using evBatteryLevel + evBatteryCapacity as context
                    val state = uiState.value
                    val level = state.evBatteryLevel.toInt()
                    val capacity = state.evBatteryCapacity
                    val units = state.evBatteryUnits
                    voiceManager.speak("Battery level is $level percent. Capacity is $capacity $units")
                }

                is Command.CheckBatteryHealth -> {
                    // No VHAL field — using evBatteryCapacity + evBatteryLevel as indirect indicators
                    voiceManager.speak(buildBatteryStatusReport())
                }

                is Command.CheckEvStatus -> {
                    // VHAL fields: evBatteryLevel, isEvChargePortConnected, evChargeTime, evCharge
                    val state = uiState.value
                    val level = state.evBatteryLevel.toInt()
                    val charge = state.evCharge
                    val connected = state.isEvChargePortConnected
                    val chargeTime = state.evChargeTime
                    val range = state.range.toInt()
                    val connectionStatus = if (connected) "connected" else "not connected"
                    val chargeInfo = if (connected) "Full charge in $chargeTime minutes." else ""
                    voiceManager.speak(
                        "EV battery is at $level percent, charging at $charge percent rate. " +
                                "Charge port is $connectionStatus. " +
                                "Estimated range is $range kilometers. $chargeInfo"
                    )
                }

                // ==================== PRO: TIRES & SUSPENSION ====================

                is Command.CheckTireStatus -> {
                    // VHAL fields: tirePressures (Map<Int, Float>), isCriticallyLowTirePressure
                    voiceManager.speak(buildTireStatusReport())
                }

                is Command.CheckTireWear -> {
                    // No VHAL field — OBD2 only. isCriticallyLowTirePressure used as context
                    val critical = uiState.value.isCriticallyLowTirePressure
                    val pressureHint = if (critical) "Warning: critically low tire pressure detected." else "Tire pressure is within normal range."
                    voiceManager.speak("Tire wear data requires OBD2 connection. $pressureHint")
                }

                is Command.CheckSuspensionHealth -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Suspension health data requires OBD2 connection")
                }

                is Command.CheckWheelAlignment -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Wheel alignment data requires OBD2 connection")
                }

                is Command.CheckSteeringStatus -> {
                    // No VHAL field — OBD2 only. isTractionControlActive used as context
                    val tcs = uiState.value.isTractionControlActive
                    val tcsStatus = if (tcs) "Traction control is currently active." else "Traction control is on standby."
                    voiceManager.speak("Steering sensor data requires OBD2 connection. $tcsStatus")
                }

                // ==================== PRO: FLUIDS & FILTERS ====================

                is Command.CheckTransmissionFluid -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Transmission fluid data requires OBD2 connection")
                }

                is Command.CheckBrakeFluid -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Brake fluid data requires OBD2 connection")
                }

                is Command.CheckCoolantLevel -> {
                    // No coolantLevel in VHAL — coolantTemp used as context
                    val temp = uiState.value.coolantTemp.toInt()
                    voiceManager.speak("Coolant level data requires OBD2 connection. Current coolant temperature is $temp degrees")
                }

                is Command.CheckAirFilter -> {
                    // No VHAL field — OBD2 only
                    voiceManager.speak("Air filter data requires OBD2 connection")
                }

                // ==================== PRO: DRIVING STATS ====================

                is Command.CheckOdometer -> {
                    // VHAL field: odometer (Float)
                    val odometer = uiState.value.odometer.toInt()
                    voiceManager.speak("Total mileage is $odometer kilometers")
                }

                is Command.CheckDrivingRange -> {
                    // VHAL field: range (Float)
                    val range = uiState.value.range.toInt()
                    voiceManager.speak("Current driving range is $range kilometers")
                }

                // ==================== PRO: ENVIRONMENTAL ====================

                is Command.CheckOutsideTemperature -> {
                    // VHAL fields: outsideTemp (Float), tempUnit (String)
                    val temp = uiState.value.outsideTemp.toInt()
                    val unit = uiState.value.tempUnit.ifEmpty { "Celsius" }
                    voiceManager.speak("Outside temperature is $temp degrees $unit")
                }

                // ==================== PRO: FIND (EXTENDED) ====================

                is Command.FindService -> {
                    // Smart trigger: mention active DTCs if any present
                    val dtcCount = uiState.value.activeDTCs.size
                    val dtcHint = if (dtcCount > 0) "You have $dtcCount active fault code${if (dtcCount > 1) "s" else ""}." else ""
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("auto repair"))
                    voiceManager.speak("$dtcHint Searching for nearby service stations")
                }

                is Command.FindCarWash -> {
                    _navigationEvent.emit(NavigationEvent.OpenNavigation("car wash"))
                    voiceManager.speak("Finding car washes near you")
                }

                // ==================== FALLBACK ====================

                is Command.Unknown -> {
                    voiceManager.speak("Sorry, I didn't understand that command")
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

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
        voskManager.release()
    }
}

private fun log(message: String) = Log.d("HermiViewModel", message)