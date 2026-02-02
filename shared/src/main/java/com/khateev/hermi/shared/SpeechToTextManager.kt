package com.khateev.hermi.shared

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechToTextManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _spokenText = MutableSharedFlow<String>()
    val spokenText: SharedFlow<String> = _spokenText.asSharedFlow()

    private val _listeningState = MutableSharedFlow<Boolean>()
    val listeningState: SharedFlow<Boolean> = _listeningState.asSharedFlow()

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        } else {

        }
    }

    fun startListening() {
        if (speechRecognizer == null) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _listeningState.tryEmit(true)
    }

    override fun onBeginningOfSpeech() {  }

    override fun onRmsChanged(rmsdB: Float) {  }

    override fun onBufferReceived(buffer: ByteArray?) {  }

    override fun onEndOfSpeech() {
        _listeningState.tryEmit(false)
    }

    override fun onError(error: Int) {
        _listeningState.tryEmit(false)
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown speech recognition error"
        }
        _spokenText.tryEmit("Error: $errorMessage") // Отправляем ошибку в UI
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestResult = matches?.getOrNull(0)
        bestResult?.let {
            _spokenText.tryEmit(it)
        }
        _listeningState.tryEmit(false)
    }

    override fun onPartialResults(partialResults: Bundle?) {  }

    override fun onEvent(eventType: Int, params: Bundle?) {  }
}