package com.khateev.hermi.shared.ai

import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Label -> Command wiring, checked against the shipped assets rather than a
 * hand-copied list: labels.txt is what the model emits, CommandCatalog is what
 * the app understands, and the two must not drift apart.
 */
class ONNXAIProcessorTest {

    private val labels: List<String> =
        File("src/main/assets/labels.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }

    @Test
    fun `every model label maps to a command`() {
        val unmapped = labels.filter { CommandCatalog.commandForLabel(it) == null }
        assertTrue("labels with no command: $unmapped", unmapped.isEmpty())
    }

    @Test
    fun `catalog covers exactly the shipped label set`() {
        val catalogLabels = CommandCatalog.specs.flatMap { listOf(it.label) + it.aliases }.toSet()
        assertEquals(labels.toSet(), catalogLabels)
        assertEquals("110 labels collapse onto 102 commands", 102, CommandCatalog.specs.size)
    }

    @Test
    fun `mapping a label above the gate returns its command`() {
        for (label in labels) {
            val expected = CommandCatalog.commandForLabel(label)
            val actual = CommandMapper.mapLabelToCommand(label, 0.99f, "some text")
            assertEquals("mapping failed for $label", expected, actual)
        }
    }

    @Test
    fun `synonym labels resolve to the same command`() {
        val pairs = listOf(
            "media_play" to "media_play_music",
            "media_pause" to "media_pause_music",
            "media_prev_track" to "media_previous_track",
            "navi_zoom_in" to "navigation_zoom_in",
            "navi_zoom_out" to "navigation_zoom_out",
            "navi_mute_guidance" to "navigation_mute_guidance",
            "navi_unmute_guidance" to "navigation_unmute_guidance",
            "navi_cancel_route" to "navigation_stop_route"
        )
        for ((canonical, alias) in pairs) {
            assertEquals(
                "$alias should resolve like $canonical",
                CommandCatalog.commandForLabel(canonical),
                CommandCatalog.commandForLabel(alias)
            )
        }
    }

    @Test
    fun `confidence below the gate is rejected`() {
        val belowGate = CommandMapper.CONFIDENCE_THRESHOLD - 0.01f
        val result = CommandMapper.mapLabelToCommand("body_door_lock", belowGate, "lock the doors")
        assertTrue(result is Command.Unknown)
        assertEquals("lock the doors", (result as Command.Unknown).rawText)
    }

    @Test
    fun `label from the retired model is rejected even above the gate`() {
        val result = CommandMapper.mapLabelToCommand("check_oil_level", 1.0f, "check the oil")
        assertTrue(result is Command.Unknown)
    }

    @Test
    fun `every command carries a spoken acknowledgement and examples`() {
        for (spec in CommandCatalog.specs) {
            assertTrue("${spec.label} has no acknowledgement", spec.acknowledgement.isNotBlank())
            assertTrue("${spec.label} acknowledgement is not a sentence",
                spec.acknowledgement.endsWith("."))
            assertTrue("${spec.label} has no title", spec.title.isNotBlank())
            assertTrue("${spec.label} has no example phrasings", spec.examples.isNotEmpty())
        }
    }

    @Test
    fun `spec lookup works in both directions`() {
        for (spec in CommandCatalog.specs) {
            assertNotNull(CommandCatalog.specFor(spec.command))
            assertEquals(spec, CommandCatalog.specFor(spec.command))
            assertEquals(spec, CommandCatalog.specForLabel(spec.label))
        }
        assertNull(CommandCatalog.specFor(Command.Unknown("nothing")))
    }

    @Test
    fun `softmax produces a probability distribution`() {
        val probs = CommandMapper.softmax(floatArrayOf(2f, 1f, 0.1f))
        assertEquals(1.0f, probs.sum(), 1e-5f)
        assertTrue(probs[0] > probs[1] && probs[1] > probs[2])
    }
}
