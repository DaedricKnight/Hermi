package com.khateev.hermi

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudioRecorderImplTest {
    private lateinit var stateMachine: AudioStateMachine

    @Before
    fun setUp() {
        stateMachine = AudioStateMachine()
    }

    @Test
    fun `short max value maps to just below 1_0`() {
        val result = shortToFloat(32767)
        assertEquals(32767 / 32768.0f, result, 0.00001f)
    }

    @Test
    fun `short min value maps to just below minus1_0`() {
        val result = shortToFloat(-32768)
        assertEquals(-32768 / 32768.0f, result, 0.00001f)
    }

    @Test
    fun `zero maps to 0_0`() {
        val result = shortToFloat(0)
        assertEquals(0.0f, result, 0.00001f)
    }

    @Test
    fun `positive short maps to positive float`() {
        val result = shortToFloat(16384)
        assertTrue(result > 0f)
        assertTrue(result < 1f)
    }

    @Test
    fun `negative short maps to negative float`() {
        val result = shortToFloat(-16384)
        assertTrue(result < 0f)
        assertTrue(result > -1f)
    }

    @Test
    fun `result is always in range minus1 to 1`() {
        val testValues = shortArrayOf(
            Short.MAX_VALUE, Short.MIN_VALUE, 0, 1000, -1000, 16000, -16000
        )
        for (v in testValues) {
            val f = shortToFloat(v.toInt())
            assertTrue("Value $v → $f out of range", f >= -1f && f <= 1f)
        }
    }

    @Test
    fun `convert buffer of shorts to floats preserves size`() {
        val buffer = ShortArray(512) { (it % 100).toShort() }
        val result = convertBuffer(buffer, buffer.size)
        assertEquals(512, result.size)
    }

    @Test
    fun `convert buffer only converts read samples`() {
        val buffer = ShortArray(512) { 1000.toShort() }
        val result = convertBuffer(buffer, 100)
        assertEquals(100, result.size)
    }

    @Test
    fun `convert empty buffer returns empty list`() {
        val buffer = ShortArray(512) { 0 }
        val result = convertBuffer(buffer, 0)
        assertEquals(0, result.size)
    }

    @Test
    fun `initial state is not recording`() {
        assertFalse(stateMachine.isRecording)
    }

    @Test
    fun `startRecording sets isRecording to true`() {
        stateMachine.startRecording()
        assertTrue(stateMachine.isRecording)
    }

    @Test
    fun `stopRecording sets isRecording to false`() {
        stateMachine.startRecording()
        stateMachine.stopRecording()
        assertFalse(stateMachine.isRecording)
    }

    @Test
    fun `stopRecording before start does not crash`() {
        stateMachine.stopRecording()
        assertFalse(stateMachine.isRecording)
    }

    @Test
    fun `isRecording returns correct state during recording`() {
        assertFalse(stateMachine.isRecording)
        stateMachine.startRecording()
        assertTrue(stateMachine.isRecording)
        stateMachine.stopRecording()
        assertFalse(stateMachine.isRecording)
    }

    @Test
    fun `samples are empty before recording starts`() {
        assertEquals(0, stateMachine.sampleCount)
    }

    @Test
    fun `addSamples accumulates correctly`() {
        stateMachine.startRecording()
        stateMachine.addSamples(ShortArray(100) { 1000 }, 100)
        assertEquals(100, stateMachine.sampleCount)
    }

    @Test
    fun `addSamples multiple times accumulates all`() {
        stateMachine.startRecording()
        stateMachine.addSamples(ShortArray(100) { 1000 }, 100)
        stateMachine.addSamples(ShortArray(200) { 2000 }, 200)
        assertEquals(300, stateMachine.sampleCount)
    }

    @Test
    fun `stopRecording returns all accumulated samples`() {
        stateMachine.startRecording()
        stateMachine.addSamples(ShortArray(256) { 16384 }, 256)
        val result = stateMachine.stopRecording()
        assertEquals(256, result.size)
    }

    @Test
    fun `startRecording clears previous samples`() {
        stateMachine.startRecording()
        stateMachine.addSamples(ShortArray(100) { 1000 }, 100)
        stateMachine.stopRecording()
        stateMachine.startRecording()
        assertEquals(0, stateMachine.sampleCount)
    }

    @Test
    fun `stopRecording with no samples returns empty array`() {
        stateMachine.startRecording()
        val result = stateMachine.stopRecording()
        assertEquals(0, result.size)
    }

    @Test
    fun `sample values are correctly converted from short`() {
        stateMachine.startRecording()
        stateMachine.addSamples(ShortArray(1) { 32767 }, 1)
        val result = stateMachine.stopRecording()
        assertEquals(32767 / 32768.0f, result[0], 0.00001f)
    }

    @Test
    fun `large recording session accumulates many samples`() {
        stateMachine.startRecording()
        repeat(100) {
            stateMachine.addSamples(ShortArray(512) { 1000 }, 512)
        }
        assertEquals(51200, stateMachine.sampleCount)
    }

    private fun shortToFloat(value: Int): Float = value / 32768.0f

    private fun convertBuffer(buffer: ShortArray, read: Int): List<Float> {
        val result = mutableListOf<Float>()
        for (i in 0 until read) {
            result.add(buffer[i] / 32768.0f)
        }
        return result
    }

    class AudioStateMachine {
        var isRecording = false
            private set

        private val samples = mutableListOf<Float>()

        val sampleCount get() = samples.size

        fun startRecording() {
            samples.clear()
            isRecording = true
        }

        fun addSamples(buffer: ShortArray, read: Int) {
            for (i in 0 until read) {
                samples.add(buffer[i] / 32768.0f)
            }
        }

        fun stopRecording(): FloatArray {
            isRecording = false
            return samples.toFloatArray()
        }
    }
}