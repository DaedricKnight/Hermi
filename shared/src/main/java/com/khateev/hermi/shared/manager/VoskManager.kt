package com.khateev.hermi.shared.manager

import android.content.Context
import android.util.Log
import com.khateev.hermi.shared.interfaces.RecognitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream

class VoskManager(private val context: Context): RecognitionManager {

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    companion object {
        private const val TAG = "VoskManager"
        private const val SAMPLE_RATE = 16000f
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
    }

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Vosk...")

            val modelDir = getModelDirectory()

            Log.d(TAG, "Loading model from: ${modelDir.absolutePath}")
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, SAMPLE_RATE)

            Log.d(TAG, "Vosk initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk", e)
            false
        }
    }

    override fun recognizeAudio(audioData: FloatArray): String? {
        val rec = recognizer ?: return null

        return try {
            val shortData = ShortArray(audioData.size) {
                (audioData[it] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }

            if (rec.acceptWaveForm(shortData, shortData.size)) {
                val result = rec.result
                parseResult(result)
            } else {
                val final = rec.result
                parseResult(final)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error", e)
            null
        }
    }

    override fun release() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        Log.d(TAG, "Vosk released")
    }

    private fun getModelDirectory(): File {
        val modelDir = File(context.filesDir, MODEL_NAME)

        if (!modelDir.exists()) {
            Log.d(TAG, "🔄 Copying model from assets...")
            copyModelFromAssets(modelDir)
        }

        return modelDir
    }

    private fun copyModelFromAssets(modelDir: File) {
        try {
            val assetPath = "models/$MODEL_NAME"

            copyAssetFolder(assetPath, modelDir)

            Log.d(TAG, "Model copied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model", e)
            throw IllegalStateException(
                "Model files missing."
            )
        }
    }

    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        targetDir.mkdirs()

        val files = context.assets.list(assetPath) ?: emptyArray()

        if (files.isEmpty()) {
            Log.d(TAG, "📄 Copying file: $assetPath")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            files.forEach { fileName ->
                val assetFilePath = "$assetPath/$fileName"
                val targetFile = File(targetDir, fileName)

                val subFiles = context.assets.list(assetFilePath) ?: emptyArray()

                if (subFiles.isEmpty()) {
                    Log.d(TAG, "Copying: $fileName")
                    context.assets.open(assetFilePath).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    Log.d(TAG, "opying folder: $fileName")
                    copyAssetFolder(assetFilePath, targetFile)
                }
            }
        }
    }


    private fun parseResult(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}