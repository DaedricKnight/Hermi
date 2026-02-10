package com.khateev.hermi.service

import android.content.Intent
import android.speech.RecognitionService
import android.util.Log

class HermiRecognitionService : RecognitionService() {

    override fun onStartListening(intent: Intent?, listener: Callback?) {
        Log.d("HermiRecognition", "onStartListening")
    }

    override fun onCancel(listener: Callback?) {
        Log.d("HermiRecognition", "onCancel")
    }

    override fun onStopListening(listener: Callback?) {
        Log.d("HermiRecognition", "onStopListening")
    }
}