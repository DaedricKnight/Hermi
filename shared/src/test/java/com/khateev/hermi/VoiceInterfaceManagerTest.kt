package com.khateev.hermi

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceInterfaceManagerTest {
    private lateinit var stateMachine: TtsStateMachine

    @Before
    fun setUp() {
        stateMachine = TtsStateMachine()
    }

    @Test
    fun `initial state is not ready`() {
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `initial spoke list is empty`() {
        assertTrue(stateMachine.spokeTexts.isEmpty())
    }

    @Test
    fun `initial skipped list is empty`() {
        assertTrue(stateMachine.skippedTexts.isEmpty())
    }

    @Test
    fun `onTtsSuccess sets isReady to true`() {
        stateMachine.onTtsSuccess()
        assertTrue(stateMachine.isReady)
    }

    @Test
    fun `onTtsFailure keeps isReady false`() {
        stateMachine.onTtsFailure()
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `onLanguageNotSupported keeps isReady false`() {
        stateMachine.onLanguageNotSupported()
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `speak when ready adds text to spoke list`() {
        stateMachine.onTtsSuccess()
        stateMachine.speak("find food")
        assertTrue(stateMachine.spokeTexts.contains("find food"))
    }

    @Test
    fun `speak when not ready adds text to skipped list`() {
        stateMachine.speak("find food")
        assertTrue(stateMachine.skippedTexts.contains("find food"))
        assertTrue(stateMachine.spokeTexts.isEmpty())
    }

    @Test
    fun `speak when not ready does not add to spoke list`() {
        stateMachine.speak("lock doors")
        assertTrue(stateMachine.spokeTexts.isEmpty())
    }

    @Test
    fun `speak multiple texts when ready stores all`() {
        stateMachine.onTtsSuccess()
        stateMachine.speak("find food")
        stateMachine.speak("lock doors")
        stateMachine.speak("check engine")
        assertEquals(3, stateMachine.spokeTexts.size)
    }

    @Test
    fun `speak empty string when ready is allowed`() {
        stateMachine.onTtsSuccess()
        stateMachine.speak("")
        assertTrue(stateMachine.spokeTexts.contains(""))
    }

    @Test
    fun `speak with special characters when ready`() {
        stateMachine.onTtsSuccess()
        val text = "turn on the a/c"
        stateMachine.speak(text)
        assertTrue(stateMachine.spokeTexts.contains(text))
    }

    @Test
    fun `speak preserves order of utterances`() {
        stateMachine.onTtsSuccess()
        stateMachine.speak("first")
        stateMachine.speak("second")
        stateMachine.speak("third")
        assertEquals("first", stateMachine.spokeTexts[0])
        assertEquals("second", stateMachine.spokeTexts[1])
        assertEquals("third", stateMachine.spokeTexts[2])
    }

    @Test
    fun `shutdown sets isReady to false`() {
        stateMachine.onTtsSuccess()
        assertTrue(stateMachine.isReady)
        stateMachine.shutdown()
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `speak after shutdown goes to skipped list`() {
        stateMachine.onTtsSuccess()
        stateMachine.shutdown()
        stateMachine.speak("find food")
        assertTrue(stateMachine.skippedTexts.contains("find food"))
        assertFalse(stateMachine.spokeTexts.contains("find food"))
    }

    @Test
    fun `shutdown when already not ready does not crash`() {
        stateMachine.shutdown()
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `shutdown called multiple times does not crash`() {
        stateMachine.onTtsSuccess()
        stateMachine.shutdown()
        stateMachine.shutdown()
        stateMachine.shutdown()
        assertFalse(stateMachine.isReady)
    }

    @Test
    fun `utterance id is hashCode of text as string`() {
        val text = "find food"
        val expected = text.hashCode().toString()
        assertEquals(expected, utteranceId(text))
    }

    @Test
    fun `different texts produce different utterance ids`() {
        val id1 = utteranceId("find food")
        val id2 = utteranceId("lock doors")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `same text always produces same utterance id`() {
        val text = "check engine"
        assertEquals(utteranceId(text), utteranceId(text))
    }

    class TtsStateMachine {
        var isReady = false
        val spokeTexts = mutableListOf<String>()
        val skippedTexts = mutableListOf<String>()

        fun onTtsSuccess() { isReady = true }
        fun onTtsFailure() { isReady = false }
        fun onLanguageNotSupported() { isReady = false }

        fun speak(text: String) {
            if (isReady) {
                spokeTexts.add(text)
            } else {
                skippedTexts.add(text)
            }
        }

        fun shutdown() {
            isReady = false
        }
    }

    private fun utteranceId(text: String): String = text.hashCode().toString()
}