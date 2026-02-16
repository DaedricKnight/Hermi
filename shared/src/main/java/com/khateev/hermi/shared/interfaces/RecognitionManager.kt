package com.khateev.hermi.shared.interfaces

interface RecognitionManager {
    suspend fun initialize(): Boolean
    fun recognizeAudio(audioData: FloatArray): String?
    fun release()
}