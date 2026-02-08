package com.khateev.hermi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    private fun checkAndRequestMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d("HermiAssistant", "Request mic permission")
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            Log.d("HermiAssistant", "Already hve permission, starting STT")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestMicrophone()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            "android.car.permission.CAR_INFO"
        ), 101)

        setContent {
//                HermiParamsScreen()
        }
    }
}