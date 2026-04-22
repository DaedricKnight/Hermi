package com.khateev.hermi.shared.ai

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SileroVAD(private val context: Context) {
    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    private var hasLoggedInfo = false
    
    // Silero v5 state: [2, 1, 128]
    private var state = FloatArray(256) { 0f }

    private fun ensureInitialized(): Boolean {
        if (isInitialized) return ortSession != null
        try {
            val modelBytes = context.assets.open("silero_vad.onnx").readBytes()
            ortSession = ortEnv.createSession(modelBytes)
            Log.d("SileroVAD", "[VAD_V4] ONNX Session created successfully (v5). Inputs: ${ortSession?.inputNames}")
        } catch (e: Throwable) {
            Log.e("SileroVAD", "[VAD_V4] CRITICAL: Silero VAD init failed: ${e.message}", e)
            ortSession = null
        }
        isInitialized = true
        return ortSession != null
    }

    fun predict(audioChunk: FloatArray): Float {
        if (!ensureInitialized()) return 0f
        val session = ortSession ?: return 0f

        // Log expected input shapes once for diagnostic purposes
        if (!hasLoggedInfo) {
            session.inputInfo.forEach { (name, info) ->
                val shape = (info.info as? TensorInfo)?.shape
                Log.d("SileroVAD", "[VAD_V4] DEBUG: Model input '$name' expects shape: ${shape?.contentToString()}")
            }
            hasLoggedInfo = true
        }
        
        return try {
            // 1. Audio Input [1, samples]
            val audioBuffer = ByteBuffer.allocateDirect(audioChunk.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            audioBuffer.put(audioChunk).rewind()
            val audioShape = longArrayOf(1, audioChunk.size.toLong())
            val audioTensor = OnnxTensor.createTensor(ortEnv, audioBuffer, audioShape)
            
            // 2. Sample Rate [1] - Scalar Long (rank 0) as expected by some models
            val srTensor = OnnxTensor.createTensor(ortEnv, 16000L)
            
            // 3. State [2, 1, 128]
            val stateBuffer = ByteBuffer.allocateDirect(state.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            stateBuffer.put(state).rewind()
            val stateShape = longArrayOf(2, 1, 128)
            val stateTensor = OnnxTensor.createTensor(ortEnv, stateBuffer, stateShape)

            val inputs = mapOf(
                "input" to audioTensor,
                "sr" to srTensor,
                "state" to stateTensor
            )

            // Use session.run in a use block to close OrtSession.Result automatically
            val probability = session.run(inputs).use { results ->
                val outputValue = results[0].value
                
                // Extract probability (prob)
                val prob = when (outputValue) {
                    is Array<*> -> (outputValue as Array<FloatArray>)[0][0]
                    is FloatArray -> outputValue[0]
                    else -> 0f
                }

                // Update internal state
                if (results.size() > 1) {
                    val nextStateValue = results[1].value
                    if (nextStateValue is Array<*>) {
                        val nextStateData = nextStateValue as Array<Array<FloatArray>>
                        for (i in 0 until 128) {
                            state[i] = nextStateData[0][0][i]
                            state[128 + i] = nextStateData[1][0][i]
                        }
                    }
                }
                prob
            }

            Log.d("SileroVAD", "[VAD_V4] Prob: $probability")

            audioTensor.close()
            srTensor.close()
            stateTensor.close()

            probability
        } catch (e: Exception) {
            Log.e("SileroVAD", "[VAD_V4] Prediction error: ${e.message}", e)
            0f
        }
    }

    fun reset() {
        state = FloatArray(256) { 0f }
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnv.close()
        } catch (e: Exception) {
            Log.e("SileroVAD", "Close error", e)
        }
    }
}
