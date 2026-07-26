package com.khateev.hermi.shared.ai

import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandCatalog
import kotlin.math.exp

object CommandMapper {

    /**
     * Confidence a prediction must reach before the app acts on it.
     *
     * The 110-label model has no `unknown` class, so this gate is the *only*
     * thing between out-of-domain speech and a car command. Measured on the
     * accent stand (tools/accent_stress_test.py): every day non-car utterances
     * land in the 0.4-0.6 band, while in-domain commands stay above 0.99 even
     * when the recognizer garbles a word.
     */
    const val CONFIDENCE_THRESHOLD = 0.95f

    /** The model's reject class; it has no command of its own. */
    const val NONE_LABEL = "none"

    fun mapLabelToCommand(label: String, confidence: Float, rawText: String): Command {
        if (label == NONE_LABEL || confidence < CONFIDENCE_THRESHOLD) {
            return Command.Unknown(rawText)
        }
        return CommandCatalog.commandForLabel(label) ?: Command.Unknown(rawText)
    }

    fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (logit in logits) if (logit > maxLogit) maxLogit = logit

        val exp = FloatArray(logits.size)
        var sum = 0f
        for (i in logits.indices) {
            exp[i] = exp((logits[i] - maxLogit).toDouble()).toFloat()
            sum += exp[i]
        }
        for (i in logits.indices) exp[i] /= sum
        return exp
    }
}
