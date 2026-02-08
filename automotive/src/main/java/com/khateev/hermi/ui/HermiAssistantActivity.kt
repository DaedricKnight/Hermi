package com.khateev.hermi.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiAssistantActivity : ComponentActivity() {

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
            // Здесь можно вызвать запуск распознавания, если нужно сразу
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       checkAndRequestMicrophone()

        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

        finish()
//        setContent {
//            HermiAssistantOverlay(onClose = {
//                android.util.Log.d("HermiUI", "Close clicked")
//                finish()
//            })
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("HermiAssistant", "Пользователь разрешил доступ к микрофону!")
                // Теперь можно безопасно вызывать speechRecognizer.startListening()
            } else {
                Log.e("HermiAssistant", "Пользователь отказал в доступе к микрофону.")
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}