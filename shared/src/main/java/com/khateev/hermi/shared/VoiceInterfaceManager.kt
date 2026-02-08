package com.khateev.hermi.shared

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceInterfaceManager @Inject constructor(
    @param:ApplicationContext val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                tts?.setAudioAttributes(audioAttributes)

                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isReady = true
                    Log.d("VoiceInterfaceManager", "TTS initialized successfully, isReady = true")
                } else {
                    Log.e("VoiceInterfaceManager", "Language not supported, result = $result")
                }
            } else {
                Log.e("VoiceInterfaceManager", "TTS initialization failed, status = $status")
            }
        }
    }

    fun speak(text: String) {
        Log.d("VoiceInterfaceManager", "speak() called, text: $text, isReady: $isReady")
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
            Log.d("VoiceInterfaceManager", "TTS speaking: $text")
        } else {
            Log.w("VoiceInterfaceManager", "TTS not ready yet, skipping: $text")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
    }
}