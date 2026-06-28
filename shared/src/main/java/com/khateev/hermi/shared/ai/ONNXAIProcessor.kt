package com.khateev.hermi.shared.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.model.Command
import java.nio.LongBuffer

class ONNXAIProcessor(private val context: Context) : AIProcessor {

    private var session: OrtSession? = null
    private val env = OrtEnvironment.getEnvironment()
    private val vocab = mutableMapOf<String, Int>()
    private val labels = mutableListOf<String>()
    private val MAX_SEQ_LEN = 32

    companion object {
        private const val TAG = "ONNXAIProcessor"
        private const val MODEL_FILE = "automotive_intent_quant.onnx"
        private const val VOCAB_FILE = "vocab.txt"
        private const val LABELS_FILE = "labels.txt"
    }

    init {
        initialize()
    }

    private fun initialize(): Boolean {
        return try {
            val modelFile = java.io.File(context.cacheDir, MODEL_FILE)
            
            // Copy asset to cache for memory-efficient session creation
            context.assets.open(MODEL_FILE).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            session = env.createSession(modelFile.absolutePath)

            loadVocab()
            loadLabels()

            Log.d(TAG, "ONNX session and vocabulary initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Processor: ${e.message}", e)
            false
        }
    }

    private fun loadVocab() {
        context.assets.open(VOCAB_FILE).bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, s -> vocab[s] = index }
        }
    }

    private fun loadLabels() {
        labels.clear()
        context.assets.open(LABELS_FILE).bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
    }

    internal fun tokenize(text: String): LongArray {
        val resultIds = mutableListOf<Long>()
        resultIds.add(vocab["[CLS]"]?.toLong() ?: 101L)

        // Split by whitespace first, then handle each "word" for punctuation
        val initialWords = text.lowercase().trim().split(Regex("\\s+"))
        val tokens = mutableListOf<String>()
        
        for (word in initialWords) {
            var current = ""
            for (char in word) {
                if (char.isLetterOrDigit()) {
                    current += char
                } else {
                    if (current.isNotEmpty()) tokens.add(current)
                    tokens.add(char.toString())
                    current = ""
                }
            }
            if (current.isNotEmpty()) tokens.add(current)
        }

        for (token in tokens) {
            if (resultIds.size >= MAX_SEQ_LEN - 1) break

            var remaining = token
            while (remaining.isNotEmpty()) {
                var subword = ""
                var end = remaining.length

                while (end > 0) {
                    val candidate = if (remaining == token) {
                        remaining.substring(0, end)
                    } else {
                        "##" + remaining.substring(0, end)
                    }

                    if (vocab.containsKey(candidate)) {
                        subword = candidate
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

        if (resultIds.size < MAX_SEQ_LEN) {
            resultIds.add(vocab["[SEP]"]?.toLong() ?: 102L)
        }

        val finalIds = LongArray(MAX_SEQ_LEN) { 0L }
        for (i in 0 until resultIds.size.coerceAtMost(MAX_SEQ_LEN)) {
            finalIds[i] = resultIds[i]
        }
        return finalIds
    }

    override suspend fun parseCommand(input: String): Command {
        if (input.isBlank() || session == null) {
            Log.w(TAG, "Empty input or session not initialized")
            return Command.Unknown(input)
        }

        Log.d(TAG, "Parsing input: \"$input\"")

        return try {
            val inputIds = tokenize(input)
            val attentionMask = LongArray(MAX_SEQ_LEN) { if (inputIds[it] != 0L) 1L else 0L }

            val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), longArrayOf(1, MAX_SEQ_LEN.toLong()))
            val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), longArrayOf(1, MAX_SEQ_LEN.toLong()))

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            session?.run(inputs).use { results ->
                val output = results?.get(0)?.value as Array<FloatArray>
                val logits = output[0]
                val probabilities = CommandMapper.softmax(logits)
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
                val confidence = probabilities.getOrElse(maxIndex) { 0f }

                val label = labels.getOrElse(maxIndex) { "unknown" }
                Log.d(TAG, "Inference result: \"$label\" with confidence ${"%.2f".format(confidence)}")

                val command = CommandMapper.mapLabelToCommand(label, confidence, input)
                Log.d(TAG, "Mapped command: $command")
                command
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference execution failure: ${e.message}", e)
            Command.Unknown(input)
        }
    }
}
