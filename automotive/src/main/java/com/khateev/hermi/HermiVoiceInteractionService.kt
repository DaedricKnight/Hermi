package com.khateev.hermi

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiVoiceInteractionService : android.service.voice.VoiceInteractionService() {

    override fun onReady() {
        super.onReady()

    }


    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        showSession(Bundle(), 0)
    }

}