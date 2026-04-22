package com.khateev.hermi.shared.interfaces

interface RecognitionManager {
    suspend fun initialize(): Boolean
    fun acceptChunk(audioData: FloatArray): Boolean
    fun recognizeAudio(audioData: FloatArray): String?
    fun release()
}
