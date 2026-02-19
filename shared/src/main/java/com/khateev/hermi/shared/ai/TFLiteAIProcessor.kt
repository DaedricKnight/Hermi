package com.khateev.hermi.shared.ai

import android.content.Context
import android.util.Log
import com.khateev.hermi.shared.HermiAIProcessor
import com.khateev.hermi.shared.interfaces.AIProcessor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteAIProcessor(private val context: Context) : AIProcessor {

    private var interpreter: Interpreter? = null
    private val vocab = mutableMapOf<String, Int>()
    private val MAX_SEQ_LEN = 32

    companion object {
        private const val TAG = "TFLiteAIProcessor"
        private const val MODEL_FILE = "mobilebert_sim_float32.tflite"
        private const val VOCAB_FILE = "vocab.txt"
    }

    init {
        initialize()
    }

    private fun initialize(): Boolean {
        return try {
            val model = loadModelFile()
            interpreter = Interpreter(model)

            loadVocab()

            Log.d(TAG, "TFLite interpreter and vocab initialized")
            checkInputs()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite", e)
            false
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun loadVocab() {
        context.assets.open(VOCAB_FILE).bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, s -> vocab[s] = index }
        }
    }

    private fun tokenize(text: String): IntArray {
        val tokens = mutableListOf<Int>()
        tokens.add(vocab["[CLS]"] ?: 101)

        val words = text.lowercase().split(" ")
        for (word in words) {
            if (tokens.size >= MAX_SEQ_LEN - 1) break

            val id = vocab[word]
            if (id != null) {
                tokens.add(id)
            } else {
                tokens.add(vocab["[UNK]"] ?: 100)
            }
        }

        tokens.add(vocab["[SEP]"] ?: 102)

        val res = IntArray(MAX_SEQ_LEN) { 0 }
        for (i in tokens.indices) {
            if (i < MAX_SEQ_LEN) res[i] = tokens[i]
        }
        return res
    }

    override fun parseCommand(input: String): HermiAIProcessor.Command {
        if (input.isBlank()) return HermiAIProcessor.Command.Unknown(input)

        return try {
            val inputIds = tokenizeToLongArray(input)
            val attentionMask = LongArray(MAX_SEQ_LEN) { if (inputIds[it] != 0L) 1L else 0L }
            val tokenTypeIds = LongArray(MAX_SEQ_LEN) { 0L }

            // Используем ByteBuffer для прямого доступа к памяти
            // 32 (длина) * 8 (байтов в Long) = 256 байт на каждый тензор
            val idBuffer = ByteBuffer.allocateDirect(MAX_SEQ_LEN * 8).order(ByteOrder.nativeOrder())
            val maskBuffer = ByteBuffer.allocateDirect(MAX_SEQ_LEN * 8).order(ByteOrder.nativeOrder())
            val typeBuffer = ByteBuffer.allocateDirect(MAX_SEQ_LEN * 8).order(ByteOrder.nativeOrder())

            for (i in 0 until MAX_SEQ_LEN) {
                idBuffer.putLong(inputIds[i])
                maskBuffer.putLong(attentionMask[i])
                typeBuffer.putLong(tokenTypeIds[i])
            }

            val output = Array(1) { FloatArray(20) }

            // Порядок строго по логам: 0-Mask, 1-IDs, 2-Types
            val inputMap = arrayOf(maskBuffer, idBuffer, typeBuffer)
            val outputMap = mapOf(0 to output)

            interpreter?.runForMultipleInputsOutputs(inputMap, outputMap)

            val probabilities = softmax(output[0])
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val confidence = probabilities.getOrElse(maxIndex) { 0f }

            Log.d(TAG, "Inference results: index $maxIndex (conf: $confidence)")

            mapIndexToCommand(maxIndex, confidence, input)

        } catch (e: Exception) {
            Log.e(TAG, "Inference failure", e)
            HermiAIProcessor.Command.Unknown(input)
        }
    }

    fun checkInputs() {
        val count = interpreter?.inputTensorCount ?: 0
        for (i in 0 until count) {
            val name = interpreter?.getInputTensor(i)?.name()
            Log.d("TFLiteAI", "Input №$i calls: $name")
        }
    }

    private fun mapIndexToCommand(index: Int, confidence: Float, rawText: String): HermiAIProcessor.Command {
        if (confidence < 0.7f) {
            Log.w(TAG, "Low confidence ($confidence) for index $index. Returning Unknown.")
            return HermiAIProcessor.Command.Unknown(rawText)
        }

        return when (index) {
            0  -> HermiAIProcessor.Command.CheckBatteryLevel
            1  -> HermiAIProcessor.Command.CheckEngine
            2  -> HermiAIProcessor.Command.CheckFuelLevel
            3  -> HermiAIProcessor.Command.CheckTires
            4  -> HermiAIProcessor.Command.FindChargingStation
            5  -> HermiAIProcessor.Command.FindFood
            6  -> HermiAIProcessor.Command.FindGasStation
            7  -> HermiAIProcessor.Command.FindParking
            8  -> HermiAIProcessor.Command.GetOdometer
            9  -> HermiAIProcessor.Command.GetOutsideTemp
            10 -> HermiAIProcessor.Command.GetRange
            11 -> HermiAIProcessor.Command.GetSpeed
            12 -> HermiAIProcessor.Command.LockDoors
            13 -> HermiAIProcessor.Command.SwitchToMock
            14 -> HermiAIProcessor.Command.SwitchToReal
            15 -> HermiAIProcessor.Command.TurnOffLights
            16 -> HermiAIProcessor.Command.TurnOnLights
            17 -> HermiAIProcessor.Command.Unknown(rawText)
            18 -> HermiAIProcessor.Command.UnlockDoors
            19 -> HermiAIProcessor.Command.VehicleStatus
            else -> HermiAIProcessor.Command.Unknown(rawText)
        }
    }
    private fun tokenizeToLongArray(text: String): LongArray {
        val resultIds = mutableListOf<Long>()

        // 1. Добавляем обязательный токен начала предложения [CLS]
        resultIds.add(vocab["[CLS]"]?.toLong() ?: 101L)

        // 2. Базовая очистка и разбиение на слова
        val words = text.lowercase().trim().split(Regex("\\s+"))

        for (word in words) {
            if (resultIds.size >= MAX_SEQ_LEN - 1) break

            // 3. Алгоритм WordPiece
            var remaining = word
            while (remaining.isNotEmpty()) {
                var subword = ""
                var end = remaining.length

                // Ищем самое длинное подслово, которое есть в словаре
                while (end > 0) {
                    val candidate =
                        if (subword.isEmpty() && resultIds.size > 1 && remaining == word) {
                            remaining.substring(0, end)
                        } else {
                            "##" + remaining.substring(0, end)
                        }

                    // Проверяем наличие в словаре (первое слово без ##, остальные с ##)
                    val testToken = if (remaining == word) remaining.substring(
                        0,
                        end
                    ) else "##" + remaining.substring(0, end)

                    if (vocab.containsKey(testToken)) {
                        subword = testToken
                        break
                    }
                    end--
                }

                if (subword.isEmpty()) {
                    resultIds.add(vocab["[UNK]"]?.toLong() ?: 100L)
                    break
                } else {
                    resultIds.add(vocab[subword]!!.toLong())
                    remaining = remaining.substring(end)
                }
            }
        }

        // 4. Добавляем токен конца предложения [SEP]
        if (resultIds.size < MAX_SEQ_LEN) {
            resultIds.add(vocab["[SEP]"]?.toLong() ?: 102L)
        }

        // 5. Формируем финальный массив фиксированной длины (32) с Padding (0)
        val finalIds = LongArray(MAX_SEQ_LEN) { 0L }
        for (i in 0 until resultIds.size.coerceAtMost(MAX_SEQ_LEN)) {
            finalIds[i] = resultIds[i]
        }

        Log.d(TAG, "Tokens for '$text': ${finalIds.contentToString()}")

        return finalIds
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (logit in logits) if (logit > maxLogit) maxLogit = logit

        val exp = FloatArray(logits.size)
        var sum = 0f
        for (i in logits.indices) {
            exp[i] = Math.exp((logits[i] - maxLogit).toDouble()).toFloat()
            sum += exp[i]
        }
        for (i in logits.indices) exp[i] /= sum
        return exp
    }
}