package com.khateev.hermi.service

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.khateev.hermi.HermiVoiceInteractionSession
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiVoiceInteractionSessionService() : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return HermiVoiceInteractionSession(this)
    }
}