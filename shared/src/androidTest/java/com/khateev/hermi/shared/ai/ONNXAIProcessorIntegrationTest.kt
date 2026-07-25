package com.khateev.hermi.shared.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shipped 110-label model on device, over the phrase bank it was trained on.
 *
 * This is a wiring and regression check — tokenizer, label order, catalog mapping
 * and confidence gate — not a generalization measurement. Accent and ASR
 * robustness live in [VoicePipelineTest] and tools/accent_stress_test.py.
 */
@RunWith(AndroidJUnit4::class)
class ONNXAIProcessorIntegrationTest {

    private lateinit var aiProcessor: ONNXAIProcessor

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        aiProcessor = ONNXAIProcessor(context)
    }

    @Test
    fun modelAccuracyOverThePhraseBank() = runTest {
        var total = 0
        var correct = 0
        val failures = mutableListOf<String>()
        val perLabel = mutableMapOf<String, IntArray>()

        for ((label, phrases) in TestPhrases.data) {
            val expected = CommandCatalog.commandForLabel(label)
                ?: error("no command for label $label")
            val counts = perLabel.getOrPut(label) { intArrayOf(0, 0) }
            for (phrase in phrases) {
                total++
                counts[1]++
                val result = aiProcessor.parseCommand(phrase)
                if (result == expected) {
                    correct++
                    counts[0]++
                } else {
                    failures.add("'$phrase' expected $label but got ${result.javaClass.simpleName}")
                }
            }
        }

        val accuracy = correct * 100.0 / total
        println("Phrase bank: $correct/$total (%.2f%%)".format(accuracy))
        perLabel.filterValues { it[0] < it[1] }.forEach { (label, c) ->
            println("  weak: $label ${c[0]}/${c[1]}")
        }
        failures.take(20).forEach { println("  FAIL $it") }

        assertTrue(
            "accuracy %.2f%% is below the 90%% floor:\n%s"
                .format(accuracy, failures.take(20).joinToString("\n")),
            accuracy >= 90.0
        )
    }

    @Test
    fun everyCommandIsReachableFromAtLeastOnePhrase() = runTest {
        val reached = mutableSetOf<Command>()
        for ((_, phrases) in TestPhrases.data) {
            for (phrase in phrases) {
                val result = aiProcessor.parseCommand(phrase)
                if (result !is Command.Unknown) reached.add(result)
            }
        }
        val unreachable = CommandCatalog.specs.map { it.command }.filterNot { it in reached }
        assertTrue(
            "commands no phrase can trigger: " +
                    unreachable.map { CommandCatalog.specFor(it)?.label }.toString(),
            unreachable.isEmpty()
        )
    }

    /**
     * Known limitation, measured rather than wished away: the 110-label model has
     * no `unknown` class, so ordinary non-car speech has nowhere to go and the
     * confidence gate is the only defence. About two thirds of off-topic phrases
     * are rejected; the rest fire a real command. Fix is on the model side —
     * train a `none` class. This test fails if the rate gets worse.
     */
    @Test
    fun outOfDomainSpeechIsMostlyRejected() = runTest {
        val offTopic = listOf(
            "what time is the meeting tomorrow", "remind me to buy milk",
            "how tall is the eiffel tower", "tell me a joke about penguins",
            "what is the capital of portugal", "send an email to my boss",
            "how do i cook rice", "what is the weather in tokyo",
            "add eggs to my shopping list", "who won the game last night",
            "translate hello into spanish", "set an alarm for seven",
            "what is my bank balance", "book a table for two",
            "how many calories in an apple", "read my messages",
            "what is the meaning of life", "play chess with me",
            "schedule a dentist appointment", "convert ten dollars to euros",
            "tell me the news headlines", "where did i leave my keys",
            "what is your name", "sing me a song", "order a pizza"
        )

        val fired = offTopic.filter { aiProcessor.parseCommand(it) !is Command.Unknown }
        val rejected = offTopic.size - fired.size
        println("Out-of-domain: $rejected/${offTopic.size} rejected " +
                "(${rejected * 100 / offTopic.size}%)")
        fired.forEach { println("  FIRES \"$it\" -> ${aiProcessor.parseCommand(it)}") }

        assertTrue(
            "off-topic rejection dropped to $rejected/${offTopic.size}; firing: $fired",
            rejected * 100 >= offTopic.size * 60
        )
    }

    @Test
    fun synonymLabelsCannotSplitACommand() = runTest {
        // "Pause the music" and "Pause playback" sit under different labels in the
        // model; both must reach the same command.
        assertEquals(Command.MediaPause, aiProcessor.parseCommand("pause the music"))
        assertEquals(Command.MediaPause, aiProcessor.parseCommand("pause playback"))
        assertEquals(Command.MediaPlay, aiProcessor.parseCommand("play music"))
        assertEquals(Command.MediaPlay, aiProcessor.parseCommand("resume playback"))
    }
}
