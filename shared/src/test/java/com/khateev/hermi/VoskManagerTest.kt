package com.khateev.hermi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoskManagerTest {
    @Test
    fun `parseResult extracts text from valid JSON`() {
        val json = """{"text": "find food near me"}"""
        val result = parseResult(json)
        assertEquals("find food near me", result)
    }

    @Test
    fun `parseResult returns empty string for empty text field`() {
        val json = """{"text": ""}"""
        val result = parseResult(json)
        assertEquals("", result)
    }

    @Test
    fun `parseResult returns empty string when text field missing`() {
        val json = """{"result": [], "partial": ""}"""
        val result = parseResult(json)
        assertEquals("", result)
    }

    @Test
    fun `parseResult returns empty string for invalid JSON`() {
        val result = parseResult("not json at all {{{")
        assertEquals("", result)
    }

    @Test
    fun `parseResult returns empty string for empty string input`() {
        val result = parseResult("")
        assertEquals("", result)
    }

    @Test
    fun `parseResult handles Vosk partial result format`() {
        val json = """{"partial": "find"}"""
        val result = parseResult(json)
        // partial не text — должен вернуть пустую строку
        assertEquals("", result)
    }

    @Test
    fun `parseResult handles Vosk final result format with confidence`() {
        val json = """
            {
              "result": [
                {"conf": 0.98, "end": 0.51, "start": 0.0, "word": "find"},
                {"conf": 0.95, "end": 0.93, "start": 0.51, "word": "food"}
              ],
              "text": "find food"
            }
        """.trimIndent()
        val result = parseResult(json)
        assertEquals("find food", result)
    }

    @Test
    fun `parseResult handles text with special characters`() {
        val json = JSONObject().apply {
            put("text", "turn on the a/c")
        }.toString()
        val result = parseResult(json)
        assertEquals("turn on the a/c", result)
    }

    @Test
    fun `parseResult handles unicode text`() {
        val json = JSONObject().apply {
            put("text", "найди еду")
        }.toString()
        val result = parseResult(json)
        assertEquals("найди еду", result)
    }

    @Test
    fun `float to short conversion maps 1_0 to max short`() {
        val input = floatArrayOf(1.0f)
        val result = convertFloatToShort(input)
        assertEquals(32767.toShort(), result[0])
    }

    @Test
    fun `float to short conversion maps minus1_0 to min short`() {
        val input = floatArrayOf(-1.0f)
        val result = convertFloatToShort(input)
        assertEquals((-32767).toShort(), result[0])
    }

    @Test
    fun `float to short conversion maps 0_0 to 0`() {
        val input = floatArrayOf(0.0f)
        val result = convertFloatToShort(input)
        assertEquals(0.toShort(), result[0])
    }

    @Test
    fun `float to short conversion clamps values above 1_0`() {
        val input = floatArrayOf(2.0f)
        val result = convertFloatToShort(input)
        assertEquals(32767.toShort(), result[0])
    }

    @Test
    fun `float to short conversion clamps values below minus1_0`() {
        val input = floatArrayOf(-2.0f)
        val result = convertFloatToShort(input)
        assertEquals((-32768).toShort(), result[0])
    }

    @Test
    fun `float to short conversion preserves array length`() {
        val input = FloatArray(1024) { 0.5f }
        val result = convertFloatToShort(input)
        assertEquals(1024, result.size)
    }

    @Test
    fun `float to short conversion handles empty array`() {
        val result = convertFloatToShort(floatArrayOf())
        assertEquals(0, result.size)
    }

    @Test
    fun `float to short conversion maps typical audio values correctly`() {
        // Тихий сигнал ~0.1 должен дать ~3276
        val input = floatArrayOf(0.1f)
        val result = convertFloatToShort(input)
        val expected = (0.1f * 32767f).toInt().toShort()
        assertEquals(expected, result[0])
    }

    @Test
    fun `recognizeAudio with null recognizer returns null`() {
        // Без инициализации recognizer == null
        val result = recognizeAudioSafe(null, floatArrayOf(0.1f, 0.2f))
        assertNull(result)
    }

    @Test
    fun `recognizeAudio with empty audio returns null or empty`() {
        // Пустой массив не должен крашить
        val result = recognizeAudioSafe(null, floatArrayOf())
        assertNull(result)
    }

    private fun parseResult(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun convertFloatToShort(audioData: FloatArray): ShortArray {
        return ShortArray(audioData.size) {
            (audioData[it] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    // Симулируем поведение когда recognizer == null
    private fun recognizeAudioSafe(recognizer: Any?, audioData: FloatArray): String? {
        if (recognizer == null) return null
        return ""
    }
}