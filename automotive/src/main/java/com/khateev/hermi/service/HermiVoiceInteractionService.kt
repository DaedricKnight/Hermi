package com.khateev.hermi.service

import android.content.ComponentName
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        log("Service is ready")

        if (!isActiveService(this, ComponentName(this, this::class.java))) {
            log("Hermi is NOT the active assistant!")
        }
    }

    /**
     * Called when the user holds the microphone button on the steering wheel
     * or says "Hey Hermi" (if hotword is configured)
     */
    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        startAssistantSession()
    }

    /**
     * Main session launch method
     */
    private fun startAssistantSession() {
        val args = Bundle().apply {
            putString("source", "hardware_button")
        }
        showSession(args, VoiceInteractionSession.SHOW_WITH_ASSIST)
    }
}

private fun log(message: String) = Log.d("HermiVoiceInteractionService", message)