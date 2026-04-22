package com.khateev.hermi.shared.interfaces

import kotlinx.coroutines.flow.SharedFlow

interface AudioRecorder {
    val audioFlow: SharedFlow<FloatArray>
    suspend fun startRecording()
    fun stopRecording(): FloatArray
    fun isRecording(): Boolean
}
