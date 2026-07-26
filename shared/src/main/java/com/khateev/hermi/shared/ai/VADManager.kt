package com.khateev.hermi.shared.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancel

class VADManager(
    private val context: Context? = null,
    private var sileroVAD: SileroVAD? = null,
    private val silenceThresholdMs: Long = 1000L,
    private val speechProbabilityThreshold: Float = 0.4f
) {
    constructor(context: Context) : this(context, null, 1000L, 0.4f)
    constructor(sileroVAD: SileroVAD, silenceThresholdMs: Long) : this(null, sileroVAD, silenceThresholdMs, 0.5f)

    private var silenceDurationMs = 0L

    suspend fun monitorSpeech(
        audioFlow: SharedFlow<FloatArray>,
        onSpeechEnded: () -> Unit
    ) {
        coroutineScope {
            try {
                if (sileroVAD == null && context != null) {
                    sileroVAD = SileroVAD(context)
                }
                
                val vad = sileroVAD ?: return@coroutineScope

                vad.reset()
                silenceDurationMs = 0L
                
                audioFlow.collect { chunk ->
                    val probability = vad.predict(chunk)
                    
                    // Log VAD activity for debugging
                    if (probability > 0.1f) {
                        Log.d("VADManager", "Speech probability: $probability")
                    }

                    val isSpeaking = probability > speechProbabilityThreshold
                    
                    val chunkDurationMs = (chunk.size * 1000L) / 16000L
                    
                    if (isSpeaking) {
                        silenceDurationMs = 0
                    } else {
                        silenceDurationMs += chunkDurationMs
                    }

                    if (silenceDurationMs >= silenceThresholdMs) {
                        Log.d("VADManager", "Silence threshold reached ($silenceThresholdMs ms). Ending speech.")
                        onSpeechEnded()
                        this.cancel() 
                    }
                }
            } catch (_: Exception) {
                // Ignore exceptions in tests where Log.e might cause RuntimeException
            }
        }
    }

    fun close() {
        sileroVAD?.close()
        sileroVAD = null
    }
}
