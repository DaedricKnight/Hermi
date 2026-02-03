package com.khateev.hermi

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiVoiceInteractionSessionService() : android.service.voice.VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): android.service.voice.VoiceInteractionSession {
        return HermiVoiceInteractionSession(this)
    }
}