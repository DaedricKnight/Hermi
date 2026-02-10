package com.khateev.hermi.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HermiAssistantActivity : ComponentActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    private fun checkAndRequestMicrophone() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            Log.d("HermiAssistant", "Request permission for mic")
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            Log.d("HermiAssistant", "Start STT")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       checkAndRequestMicrophone()

        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("HermiAssistant", "Granted")
            } else {
                Log.e("HermiAssistant", "Not granted")
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}