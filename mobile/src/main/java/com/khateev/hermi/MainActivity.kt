package com.khateev.hermi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            "android.car.permission.CAR_INFO"
        ), 101)

        setContent {
            // Твоя тема оформления
                // Тот самый экран, который мы создали
                HermiParamsScreen()
        }
    }
}