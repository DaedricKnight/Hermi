package com.khateev.hermi.shared.ai

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.*

class VADManagerTest {

    @Test
    fun testVADManagerDetectsSpeechEndAfterSilence() = runTest {
        val mockSileroVAD = mockk<SileroVAD>(relaxed = true)
        val vadManager = VADManager(mockSileroVAD, 100L) 
        
        val audioFlow = MutableSharedFlow<FloatArray>(10)
        var speechEndedCalled = false

        val job = launch {
            vadManager.monitorSpeech(audioFlow) {
                speechEndedCalled = true
            }
        }

        // Active speech
        every { mockSileroVAD.predict(any()) } returns 0.9f
        audioFlow.emit(FloatArray(512)) 
        yield()
        assertTrue(!speechEndedCalled)

        // Silence (128ms total)
        every { mockSileroVAD.predict(any()) } returns 0.1f
        repeat(4) {
            audioFlow.emit(FloatArray(512))
            yield()
        }

        assertTrue("Speech end should be called", speechEndedCalled)
        job.cancel()
    }

    @Test
    fun testVADManagerResetsSilenceWhenSpeechOccurs() = runTest {
        val mockSileroVAD = mockk<SileroVAD>(relaxed = true)
        val vadManager = VADManager(mockSileroVAD, 100L)
        
        val audioFlow = MutableSharedFlow<FloatArray>(10)
        var speechEndedCalled = false

        val job = launch {
            vadManager.monitorSpeech(audioFlow) {
                speechEndedCalled = true
            }
        }

        // 2 chunks of silence (64ms)
        every { mockSileroVAD.predict(any()) } returns 0.1f
        repeat(2) { audioFlow.emit(FloatArray(512)) }
        yield()
        assertTrue(!speechEndedCalled)
        
        // 1 chunk of speech (32ms) -> reset
        every { mockSileroVAD.predict(any()) } returns 0.9f
        audioFlow.emit(FloatArray(512))
        yield()
        assertTrue(!speechEndedCalled)

        // 2 chunks of silence (64ms) -> total 64ms, < 100ms
        every { mockSileroVAD.predict(any()) } returns 0.1f
        repeat(2) { audioFlow.emit(FloatArray(512)) }
        yield()
        assertTrue(!speechEndedCalled)
        
        // 2 more chunks (total 128ms) -> finish
        repeat(2) { audioFlow.emit(FloatArray(512)) }
        yield()
        
        assertTrue("Speech end should be called now", speechEndedCalled)
        job.cancel()
    }
}
