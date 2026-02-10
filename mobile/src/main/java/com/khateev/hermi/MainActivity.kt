package com.khateev.hermi

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
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            Log.d("HermiAssistant", "Запрашиваем разрешение на микрофон...")
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            Log.d("HermiAssistant", "Разрешение уже есть, стартуем STT")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestMicrophone()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            "android.car.permission.CAR_INFO"
        ), 101)

        setContent {
        }
    }
}