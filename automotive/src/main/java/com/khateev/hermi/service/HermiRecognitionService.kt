package com.khateev.hermi.service

import android.content.Intent
import android.speech.RecognitionService
import android.util.Log

class HermiRecognitionService : RecognitionService() {

    override fun onStartListening(intent: Intent?, listener: Callback?) {
        log("onStartListening")
    }

    override fun onCancel(listener: Callback?) {
        log("onCancel")
    }

    override fun onStopListening(listener: Callback?) {
        log("onStopListening")
    }
}

private fun log(message: String) = Log.d("HermiRecognition", message)