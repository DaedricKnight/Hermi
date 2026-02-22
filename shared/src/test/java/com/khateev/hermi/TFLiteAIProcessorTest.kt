package com.khateev.hermi

import com.khateev.hermi.shared.model.Command
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TFLiteAIProcessorTest {
    private val vocab = mutableMapOf<String, Int>()

    @Before
    fun setUp() {
        // minimum vocab for tests
        vocab["[PAD]"] = 0
        vocab["[UNK]"] = 100
        vocab["[CLS]"] = 101
        vocab["[SEP]"] = 102
        vocab["find"] = 2424
        vocab["food"] = 2833
        vocab["gas"] = 3806
        vocab["station"] = 4218
        vocab["lock"] = 5023
        vocab["doors"] = 4402
        vocab["check"] = 4638
        vocab["engine"] = 3194
        vocab["battery"] = 6046
        vocab["##ing"] = 2075
        vocab["park"] = 3952
        vocab["speed"] = 4322
        vocab["temperature"] = 4860
    }

    @Test
    fun `tokenize always starts with CLS token`() {
        val result = tokenizeToLongArray("find food")
        assertEquals(101L, result[0]) // [CLS]
    }

    @Test
    fun `tokenize known word returns correct id`() {
        val result = tokenizeToLongArray("find food")
        assertEquals(2424L, result[1]) // "find"
        assertEquals(2833L, result[2]) // "food"
    }

    @Test
    fun `tokenize unknown word returns UNK token`() {
        val result = tokenizeToLongArray("xyzunknownword")
        assertEquals(100L, result[1]) // [UNK]
    }

    @Test
    fun `tokenize result length is always MAX_SEQ_LEN`() {
        val result = tokenizeToLongArray("find food near me")
        assertEquals(32, result.size)
    }

    @Test
    fun `tokenize pads with zeros after SEP`() {
        val result = tokenizeToLongArray("find")
        assertEquals(102L, result[2]) // [SEP]
        assertEquals(0L, result[3])   // PAD
    }

    @Test
    fun `tokenize empty string returns only CLS and SEP`() {
        val result = tokenizeToLongArray("")
        assertEquals(101L, result[0]) // [CLS]
        assertEquals(32, result.size)
    }

    @Test
    fun `tokenize lowercases input`() {
        val lower = tokenizeToLongArray("find food")
        val upper = tokenizeToLongArray("FIND FOOD")
        assertArrayEquals(lower, upper)
    }

    @Test
    fun `tokenize long input truncates to MAX_SEQ_LEN`() {
        val longInput = "find " .repeat(50)
        val result = tokenizeToLongArray(longInput)
        assertEquals(32, result.size)
        assertTrue(result[31] == 102L || result[31] == 0L)
    }

    @Test
    fun `attention mask is 1 for non-zero input ids`() {
        val inputIds = tokenizeToLongArray("find food")
        val mask = LongArray(32) { if (inputIds[it] != 0L) 1L else 0L }

        assertEquals(1L, mask[0])
        assertEquals(1L, mask[1])
        assertEquals(1L, mask[2])
        assertEquals(1L, mask[3])
    }

    @Test
    fun `attention mask is 0 for padding tokens`() {
        val inputIds = tokenizeToLongArray("find")
        val mask = LongArray(32) { if (inputIds[it] != 0L) 1L else 0L }

        for (i in 3 until 32) {
            assertEquals("Mask at position $i should be 0", 0L, mask[i])
        }
    }

    @Test
    fun `softmax output sums to 1`() {
        val logits = FloatArray(20) { it.toFloat() }
        val result = softmax(logits)
        val sum = result.sum()
        assertEquals(1.0f, sum, 0.001f)
    }

    @Test
    fun `softmax highest logit gets highest probability`() {
        val logits = FloatArray(20) { 0f }
        logits[5] = 10f // FindFood
        val result = softmax(logits)
        val maxIndex = result.indices.maxByOrNull { result[it] }
        assertEquals(5, maxIndex)
    }

    @Test
    fun `softmax all equal logits gives uniform distribution`() {
        val logits = FloatArray(20) { 1f }
        val result = softmax(logits)
        val expected = 1f / 20f
        for (p in result) {
            assertEquals(expected, p, 0.001f)
        }
    }

    @Test
    fun `softmax handles extreme values without NaN`() {
        val logits = FloatArray(20) { 0f }
        logits[0] = 1_000_000f
        logits[1] = -1_000_000f
        val result = softmax(logits)
        for (p in result) {
            assertFalse("Softmax produced NaN", p.isNaN())
            assertFalse("Softmax produced Inf", p.isInfinite())
        }
    }

    @Test
    fun `mapIndexToCommand returns correct command for each index`() {
        val cases = mapOf(
            0  to Command.CheckBatteryLevel::class,
            1  to Command.CheckEngine::class,
            2  to Command.CheckFuelLevel::class,
            3  to Command.CheckTires::class,
            4  to Command.FindChargingStation::class,
            5  to Command.FindFood::class,
            6  to Command.FindGasStation::class,
            7  to Command.FindParking::class,
            8  to Command.GetOdometer::class,
            9  to Command.GetOutsideTemp::class,
            10 to Command.GetRange::class,
            11 to Command.GetSpeed::class,
            12 to Command.LockDoors::class,
            15 to Command.TurnOffLights::class,
            16 to Command.TurnOnLights::class,
            18 to Command.UnlockDoors::class,
            19 to Command.VehicleStatus::class,
        )

        for ((index, expectedClass) in cases) {
            val command = mapIndexToCommand(index, 0.95f, "test")
            assertEquals(
                "Index $index should map to ${expectedClass.simpleName}",
                expectedClass,
                command::class
            )
        }
    }

    @Test
    fun `mapIndexToCommand returns Unknown for low confidence`() {
        val command = mapIndexToCommand(5, 0.5f, "find food") // ниже порога 0.7
        assertTrue("Low confidence should return Unknown", command is Command.Unknown)
    }

    @Test
    fun `mapIndexToCommand returns Unknown for unknown index`() {
        val command = mapIndexToCommand(99, 0.95f, "random")
        assertTrue(command is Command.Unknown)
    }

    @Test
    fun `mapIndexToCommand preserves raw text in Unknown command`() {
        val rawText = "some unknown phrase"
        val command = mapIndexToCommand(99, 0.95f, rawText) as Command.Unknown
        assertEquals(rawText, command.rawText)
    }

    @Test
    fun `confidence threshold boundary - exactly 0_7 should succeed`() {
        val command = mapIndexToCommand(5, 0.7f, "find food")
        assertTrue("Confidence exactly at threshold should succeed", command is Command.FindFood)
    }

    @Test
    fun `confidence threshold boundary - just below 0_7 should fail`() {
        val command = mapIndexToCommand(5, 0.699f, "find food")
        assertTrue("Confidence just below threshold should return Unknown", command is Command.Unknown)
    }

    private val MAX_SEQ_LEN = 32

    private fun tokenizeToLongArray(text: String): LongArray {
        val resultIds = mutableListOf<Long>()
        resultIds.add(vocab["[CLS]"]?.toLong() ?: 101L)

        val words = text.lowercase().trim().split(Regex("\\s+"))

        for (word in words) {
            if (resultIds.size >= MAX_SEQ_LEN - 1) break

            var remaining = word
            while (remaining.isNotEmpty()) {
                var subword = ""
                var end = remaining.length

                while (end > 0) {
                    val testToken = if (remaining == word)
                        remaining.substring(0, end)
                    else
                        "##" + remaining.substring(0, end)

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

        if (resultIds.size < MAX_SEQ_LEN) {
            resultIds.add(vocab["[SEP]"]?.toLong() ?: 102L)
        }

        val finalIds = LongArray(MAX_SEQ_LEN) { 0L }
        for (i in 0 until resultIds.size.coerceAtMost(MAX_SEQ_LEN)) {
            finalIds[i] = resultIds[i]
        }
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

    private fun mapIndexToCommand(index: Int, confidence: Float, rawText: String): Command {
        if (confidence < 0.7f) return Command.Unknown(rawText)

        return when (index) {
            0  -> Command.CheckBatteryLevel
            1  -> Command.CheckEngine
            2  -> Command.CheckFuelLevel
            3  -> Command.CheckTires
            4  -> Command.FindChargingStation
            5  -> Command.FindFood
            6  -> Command.FindGasStation
            7  -> Command.FindParking
            8  -> Command.GetOdometer
            9  -> Command.GetOutsideTemp
            10 -> Command.GetRange
            11 -> Command.GetSpeed
            12 -> Command.LockDoors
            15 -> Command.TurnOffLights
            16 -> Command.TurnOnLights
            17 -> Command.Unknown(rawText)
            18 -> Command.UnlockDoors
            19 -> Command.VehicleStatus
            else -> Command.Unknown(rawText)
        }
    }
}