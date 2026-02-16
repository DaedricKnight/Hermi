package com.khateev.hermi.shared.interfaces

interface AudioRecorder {
    suspend fun startRecording()
    fun stopRecording(): FloatArray
    fun isRecording(): Boolean
}