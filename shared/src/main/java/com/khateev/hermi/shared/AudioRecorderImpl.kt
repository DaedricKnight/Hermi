package com.khateev.hermi.shared

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.khateev.hermi.shared.interfaces.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class AudioRecorderImpl: AudioRecorder {

    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false
    private val recordedSamples = mutableListOf<Float>()

    private val _audioFlow = MutableSharedFlow<FloatArray>(extraBufferCapacity = 50)
    override val audioFlow = _audioFlow.asSharedFlow()

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    override suspend fun startRecording() {
        withContext(Dispatchers.IO) {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ).coerceAtLeast(1024) * 2

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    return@withContext
                }

                recordedSamples.clear()
                isRecording = true
                audioRecord?.startRecording()

                Log.d(TAG, "Recording started")

                val buffer = ShortArray(512) 
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        val floatChunk = FloatArray(read)
                        for (i in 0 until read) {
                            val floatSample = buffer[i] / 32768.0f
                            floatChunk[i] = floatSample
                            recordedSamples.add(floatSample)
                            sum += floatSample * floatSample
                        }
                        
                        // Check volume level
                        val rms = sqrt(sum / read)
                        if (rms > 0.01) {
                             // Log significant audio levels for debugging
                              Log.d(TAG, "Audio Level (RMS): $rms")
                        }
                        
                        // Emit float array to the shared flow
                        _audioFlow.tryEmit(floatChunk)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording error: ${e.message}", e)
                isRecording = false
            } finally {
                stopInternal()
            }
        }
    }

    override fun stopRecording(): FloatArray {
        isRecording = false
        stopInternal()
        return synchronized(recordedSamples) {
            recordedSamples.toFloatArray()
        }
    }

    private fun stopInternal() {
        try {
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED && it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping record: ${e.message}", e)
        }
        audioRecord = null
    }

    override fun isRecording(): Boolean = isRecording
}
