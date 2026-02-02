package com.khateev.hermi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class HermiViewModel @Inject constructor(
    private val speechToTextManager: SpeechToTextManager,
    private val vhalReader: VhalReader,
    private val aiProcessor: HermiAIProcessor,
    private val voiceManager: VoiceInterfaceManager
) : ViewModel() {

    val uiState: StateFlow<VhalParamsState> = vhalReader.carState

    private val _isMockMode = MutableStateFlow(true)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    val isListening: StateFlow<Boolean> = speechToTextManager.listeningState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
                _recognizedText.emit(text)
            }
        }
    }

    fun toggleVhalSource(useMock: Boolean) {
        viewModelScope.launch {
            _isMockMode.value = useMock
//            (vhalReader as? HybridVhalReader)?.toggleMode(useMock)
            val msg = if (useMock) "Simulation Mode Active" else "Real VHAL Connected"
            _events.emit(msg)
        }
    }

    fun startVoiceInput() = speechToTextManager.startListening()
    fun stopVoiceInput() = speechToTextManager.stopListening()

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
        speechToTextManager.destroy()
    }
}