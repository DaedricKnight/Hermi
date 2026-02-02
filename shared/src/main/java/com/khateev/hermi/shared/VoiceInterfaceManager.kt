package com.khateev.hermi.shared

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceInterfaceManager @Inject constructor(
    @param:ApplicationContext val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    /**
     * Text to speech
     */
    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
        }
    }

    /**
     * Clear resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
    }
}