package com.khateev.hermi.shared.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khateev.hermi.shared.model.Command
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * End-to-end voice pipeline test without a human speaker.
 *
 * Test WAVs are synthesized on the host with macOS TTS (`tools/generate_test_wavs.sh`,
 * 16 kHz mono PCM16) in seven accents and bundled as androidTest assets. Each file
 * runs through the exact production chain — Vosk recognition -> ONNX intent
 * classification -> CommandMapper's confidence gate — bypassing only
 * AudioRecord itself.
 *
 * The host-side counterpart (`tools/accent_stress_test.py`) sweeps the same
 * pipeline over accents x speech rates x noise; this test locks in the outcome
 * on real hardware.
 */
@RunWith(AndroidJUnit4::class)
class VoicePipelineTest {

    /** One bundled clip: the WAV, the accent it was voiced in, the command it must produce. */
    private data class Fixture(val wav: String, val accent: String, val expected: Command)

    companion object {
        private lateinit var vosk: VoskManager
        private lateinit var classifier: ONNXAIProcessor

        @BeforeClass
        @JvmStatic
        fun setUp(): Unit = runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            vosk = VoskManager(context)
            check(vosk.initialize()) { "Vosk init failed" }
            classifier = ONNXAIProcessor(context)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            vosk.release()
        }

        /** Minimal WAV reader for the known test fixtures (16 kHz mono PCM16). */
        private fun loadWavPcm(assetPath: String): FloatArray {
            val testContext = InstrumentationRegistry.getInstrumentation().context
            val bytes = testContext.assets.open(assetPath).readBytes()
            val dataOffset = findDataChunk(bytes)
            val sampleCount = (bytes.size - dataOffset) / 2
            val pcm = FloatArray(sampleCount)
            var i = dataOffset
            for (s in 0 until sampleCount) {
                val lo = bytes[i].toInt() and 0xFF
                val hi = bytes[i + 1].toInt()
                pcm[s] = ((hi shl 8) or lo) / 32768f
                i += 2
            }
            return pcm
        }

        private fun findDataChunk(bytes: ByteArray): Int {
            var pos = 12 // skip RIFF header
            while (pos + 8 <= bytes.size) {
                val id = String(bytes, pos, 4, Charsets.US_ASCII)
                val size = DataInputStream(ByteArrayInputStream(bytes, pos + 4, 4)).let {
                    val b = ByteArray(4); it.readFully(b)
                    ((b[3].toInt() and 0xFF) shl 24) or ((b[2].toInt() and 0xFF) shl 16) or
                            ((b[1].toInt() and 0xFF) shl 8) or (b[0].toInt() and 0xFF)
                }
                if (id == "data") return pos + 8
                pos += 8 + size + (size and 1)
            }
            error("no data chunk in WAV")
        }
    }

    private fun recognize(wav: String): String {
        val text = vosk.recognizeAudio(loadWavPcm("tts/$wav"))
        assertTrue("no transcript for $wav", !text.isNullOrBlank())
        return text!!
    }

    private fun runPipeline(wav: String): Pair<String, Command> = runBlocking {
        val text = recognize(wav)
        text to classifier.parseCommand(text)
    }

    private fun assertCommand(fixture: Fixture) {
        val (text, command) = runPipeline(fixture.wav)
        assertEquals(
            "${fixture.accent} ${fixture.wav} heard=\"$text\"",
            fixture.expected::class, command::class
        )
    }

    private fun assertAll(fixtures: List<Fixture>) = fixtures.forEach(::assertCommand)

    @Test
    fun usAccent_cabinCommands_areRecognized() = assertAll(
        listOf(
            Fixture("door_lock_samantha.wav", "US", Command.BodyDoorLock),
            Fixture("sunroof_open_samantha.wav", "US", Command.BodySunroofOpen),
            Fixture("ac_on_samantha.wav", "US", Command.ClimateAcOn),
            Fixture("temp_up_samantha.wav", "US", Command.ClimateIncreaseTemperature)
        )
    )

    @Test
    fun usAccent_mediaAndNavigation_areRecognized() = assertAll(
        listOf(
            Fixture("media_play_samantha.wav", "US", Command.MediaPlay),
            Fixture("volume_up_samantha.wav", "US", Command.MediaVolumeUp),
            Fixture("navi_home_samantha.wav", "US", Command.NavigationGoHome),
            Fixture("find_parking_samantha.wav", "US", Command.NavigationFindParking)
        )
    )

    @Test
    fun britishAccent_commands_areRecognized() = assertAll(
        listOf(
            Fixture("headlights_on_daniel.wav", "GB", Command.LightHeadlightsOn),
            Fixture("next_track_daniel.wav", "GB", Command.MediaNextTrack)
        )
    )

    @Test
    fun australianAccent_commands_areRecognized() = assertAll(
        listOf(
            Fixture("wipers_on_karen.wav", "AU", Command.WipersOn),
            Fixture("zoom_in_karen.wav", "AU", Command.NavigationZoomIn)
        )
    )

    @Test
    fun irishAndIndianAccents_commands_areRecognized() = assertAll(
        listOf(
            Fixture("cancel_route_moira.wav", "IE", Command.NavigationCancelRoute),
            Fixture("accept_call_rishi.wav", "IN", Command.PhoneAcceptCall),
            Fixture("radio_on_rishi.wav", "IN", Command.RadioTurnOn)
        )
    )

    @Test
    fun southAfricanAccent_commands_areRecognized() = assertAll(
        listOf(
            Fixture("rear_camera_tessa.wav", "ZA", Command.CameraShowRear),
            Fixture("parking_sensors_tessa.wav", "ZA", Command.ParkingSensorsOn)
        )
    )

    @Test
    fun slowAndFastDelivery_produceTheSameCommand() = assertAll(
        listOf(
            Fixture("door_lock_slow_daniel.wav", "GB slow 130wpm", Command.BodyDoorLock),
            Fixture("door_lock_fast_daniel.wav", "GB fast 240wpm", Command.BodyDoorLock)
        )
    )

    /** "Stop the music" trains under media_pause_music, which must not split off. */
    @Test
    fun synonymLabel_landsOnTheSameCommand() =
        assertCommand(Fixture("media_pause_samantha.wav", "US", Command.MediaPause))

    @Test
    fun outOfDomainPhrase_doesNotFireACommand() {
        val (text, command) = runPipeline("ood_milk_samantha.wav")
        assertTrue(
            "\"$text\" -> ${command.javaClass.simpleName} would trigger a false action",
            command is Command.Unknown
        )
    }

    /**
     * Aggregate sweep over every bundled accent. Prints a per-accent table to logcat
     * so a regression shows *which* accent broke, and fails only if the overall
     * pass rate drops below the level the fixtures are known to reach.
     */
    @Test
    fun accentSweep_passRateHoldsAcrossAllAccents() {
        val fixtures = listOf(
            Fixture("door_lock_samantha.wav", "US", Command.BodyDoorLock),
            Fixture("sunroof_open_samantha.wav", "US", Command.BodySunroofOpen),
            Fixture("ac_on_samantha.wav", "US", Command.ClimateAcOn),
            Fixture("temp_up_samantha.wav", "US", Command.ClimateIncreaseTemperature),
            Fixture("media_play_samantha.wav", "US", Command.MediaPlay),
            Fixture("media_pause_samantha.wav", "US", Command.MediaPause),
            Fixture("volume_up_samantha.wav", "US", Command.MediaVolumeUp),
            Fixture("navi_home_samantha.wav", "US", Command.NavigationGoHome),
            Fixture("find_parking_samantha.wav", "US", Command.NavigationFindParking),
            Fixture("headlights_on_daniel.wav", "GB", Command.LightHeadlightsOn),
            Fixture("next_track_daniel.wav", "GB", Command.MediaNextTrack),
            Fixture("door_lock_slow_daniel.wav", "GB", Command.BodyDoorLock),
            Fixture("door_lock_fast_daniel.wav", "GB", Command.BodyDoorLock),
            Fixture("wipers_on_karen.wav", "AU", Command.WipersOn),
            Fixture("zoom_in_karen.wav", "AU", Command.NavigationZoomIn),
            Fixture("cancel_route_moira.wav", "IE", Command.NavigationCancelRoute),
            Fixture("accept_call_rishi.wav", "IN", Command.PhoneAcceptCall),
            Fixture("radio_on_rishi.wav", "IN", Command.RadioTurnOn),
            Fixture("rear_camera_tessa.wav", "ZA", Command.CameraShowRear),
            Fixture("parking_sensors_tessa.wav", "ZA", Command.ParkingSensorsOn)
        )

        val failures = mutableListOf<String>()
        val perAccent = mutableMapOf<String, IntArray>() // accent -> [passed, total]

        for (fixture in fixtures) {
            val (text, command) = runPipeline(fixture.wav)
            val counts = perAccent.getOrPut(fixture.accent) { intArrayOf(0, 0) }
            counts[1]++
            if (command::class == fixture.expected::class) {
                counts[0]++
            } else {
                failures.add(
                    "${fixture.accent} ${fixture.wav}: heard=\"$text\" -> " +
                            "${command.javaClass.simpleName}, expected " +
                            fixture.expected.javaClass.simpleName
                )
            }
        }

        val passed = perAccent.values.sumOf { it[0] }
        val total = perAccent.values.sumOf { it[1] }
        println("Accent sweep: $passed/$total (${passed * 100 / total}%)")
        perAccent.toSortedMap().forEach { (accent, c) ->
            println("  $accent: ${c[0]}/${c[1]}")
        }
        failures.forEach { println("  FAIL $it") }

        assertTrue(
            "accent pass rate $passed/$total dropped below 90%:\n" + failures.joinToString("\n"),
            passed * 100 >= total * 90
        )
    }

    @Test
    fun sileroVad_scoresSpeechAboveSilence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed on purpose — SileroVAD.close() also closes the shared
        // OrtEnvironment that ONNXAIProcessor keeps a session in.
        val vad = SileroVAD(context)
        val speech = loadWavPcm("tts/ac_on_samantha.wav")
        val silence = FloatArray(SILENCE_SAMPLES)

        val speechProbability = maxProbability(vad, speech)
        vad.reset()
        val silenceProbability = maxProbability(vad, silence)

        assertTrue("speech scored only $speechProbability", speechProbability > 0.5f)
        assertTrue("silence scored $silenceProbability", silenceProbability < 0.5f)
    }

    @Test
    fun vadManager_endsSpeechAfterTrailingSilence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val speech = loadWavPcm("tts/ac_on_samantha.wav")
        val stream = speech + FloatArray(SILENCE_SAMPLES) // command, then 1.5 s of quiet

        val chunks = ArrayList<FloatArray>(stream.size / VAD_CHUNK)
        var offset = 0
        while (offset + VAD_CHUNK <= stream.size) {
            chunks.add(stream.copyOfRange(offset, offset + VAD_CHUNK))
            offset += VAD_CHUNK
        }
        val flow = MutableSharedFlow<FloatArray>(replay = chunks.size)
        chunks.forEach { check(flow.tryEmit(it)) }

        // Deliberately not calling VADManager.close()/SileroVAD.close() here: it tears
        // down the shared OrtEnvironment that ONNXAIProcessor is still holding.
        val vadManager = VADManager(SileroVAD(context), silenceThresholdMs = 1000L)
        val ended = CompletableDeferred<Unit>()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            scope.launch {
                runCatching { vadManager.monitorSpeech(flow) { ended.complete(Unit) } }
            }
            runBlocking { withTimeout(30_000) { ended.await() } }
        } finally {
            scope.cancel()
        }
    }

    private fun maxProbability(vad: SileroVAD, pcm: FloatArray): Float {
        var max = 0f
        var i = 0
        val chunk = FloatArray(VAD_CHUNK)
        while (i + VAD_CHUNK <= pcm.size) {
            pcm.copyInto(chunk, 0, i, i + VAD_CHUNK)
            max = maxOf(max, vad.predict(chunk))
            i += VAD_CHUNK
        }
        return max
    }
}

/** Silero v5 window at 16 kHz. */
private const val VAD_CHUNK = 512

/** 1.5 s of trailing silence — comfortably past VADManager's 1 s threshold. */
private const val SILENCE_SAMPLES = 24_000
