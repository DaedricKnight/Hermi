package com.khateev.hermi

import android.content.Intent
import android.speech.RecognitionService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent?, callback: Callback?) {

    }

    override fun onCancel(callback: Callback?) {}
    override fun onStopListening(callback: Callback?) {}
}