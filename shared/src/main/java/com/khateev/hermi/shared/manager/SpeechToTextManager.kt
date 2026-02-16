package com.khateev.hermi.shared.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechToTextManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RecognitionListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var speechRecognizer: SpeechRecognizer? = null

    private val _spokenText = MutableSharedFlow<String>(replay = 1)
    val spokenText: SharedFlow<String> = _spokenText.asSharedFlow()

    private val _listeningState = MutableStateFlow<Boolean>(false)
    val listeningState: StateFlow<Boolean> = _listeningState.asStateFlow()

    private fun ensureRecognizer() {
        log("ensureRecognizer start")
        if (speechRecognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(this@SpeechToTextManager)
                        }
                        log("SpeechRecognizer initialized with default service")
                    } catch (e: Exception) {
                        log("Failed to init SpeechRecognizer $e")
                    }
                }
            }
        }
    }

    fun startListening() {
        val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        log("Recognition available: $isAvailable")

        val hasPermission = context.checkSelfPermission(
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        log("RECORD_AUDIO permission: $hasPermission")

        if (!hasPermission) {
            log("No RECORD_AUDIO permission!")
            return
        }

        Handler(Looper.getMainLooper()).post {
            ensureRecognizer()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            startListeningWithFocus(intent)
        }
    }

    fun startListeningWithFocus(intent: Intent) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE) // Важно для AAOS
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            log("AudioManager.AUDIOFOCUS_REQUEST_GRANTED")

            Handler(Looper.getMainLooper()).postDelayed({
                log("Calling startListening after delay")
                speechRecognizer?.startListening(intent)
            }, 500)
        } else {
            log("Focus denied")
        }
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
        Log.d("HermiSTT", "REAL TALK: Microphone is open now!")
        _listeningState.value = true
    }

    override fun onBeginningOfSpeech() {
        _listeningState.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _listeningState.value = false
    }

    override fun onError(error: Int) {
        _listeningState.value = false
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
        _spokenText.tryEmit("Error: $errorMessage")
        log("Speech Error: $errorMessage")
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.getOrNull(0)?.let { _spokenText.tryEmit(it) }
        _listeningState.value = false
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

private fun log(message: String) = Log.d("HermiSTT", message)