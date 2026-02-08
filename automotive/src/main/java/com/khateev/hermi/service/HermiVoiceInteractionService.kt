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
        Log.d("HermiVoiceService", "Service is ready")

        if (!isActiveService(this, ComponentName(this, this::class.java))) {
            Log.w("HermiVoiceService", "Hermi is NOT the active assistant!")
        }
    }

    /**
     * Вызывается, когда пользователь зажимает кнопку микрофона на руле
     * или говорит "Hey Hermi" (если настроен hotword)
     */
    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        startAssistantSession()
    }

    /**
     * Основной метод запуска сессии
     */
    private fun startAssistantSession() {
        val args = Bundle().apply {
            putString("source", "hardware_button")
        }
        showSession(args, VoiceInteractionSession.SHOW_WITH_ASSIST)
    }
}