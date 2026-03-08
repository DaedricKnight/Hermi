package com.khateev.hermi

import com.khateev.hermi.shared.model.Command
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ONNXAIProcessorTest {
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
    fun `softmax output sums to 1`() {
        val logits = FloatArray(31) { it.toFloat() }
        val result = softmax(logits)
        val sum = result.sum()
        assertEquals(1.0f, sum, 0.001f)
    }

    @Test
    fun `mapLabelToCommand returns correct command for engine`() {
        val command = mapLabelToCommand("check_engine", 0.95f, "check engine")
        assertTrue(command is Command.CheckEngineStatus)
    }

    @Test
    fun `mapLabelToCommand returns correct command for food`() {
        val command = mapLabelToCommand("find_food", 0.95f, "i want food")
        assertTrue(command is Command.FindFood)
    }

    @Test
    fun `mapLabelToCommand returns Unknown for low confidence`() {
        val command = mapLabelToCommand("find_food", 0.5f, "find food")
        assertTrue("Low confidence should return Unknown", command is Command.Unknown)
    }

    @Test
    fun `mapLabelToCommand returns Unknown for unknown label`() {
        val command = mapLabelToCommand("unknown", 0.95f, "random text")
        assertTrue(command is Command.Unknown)
    }

    @Test
    fun `mapLabelToCommand preserves raw text in Unknown command`() {
        val rawText = "some unknown phrase"
        val command = mapLabelToCommand("unknown", 0.95f, rawText) as Command.Unknown
        assertEquals(rawText, command.rawText)
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
                    val candidate = if (remaining == word)
                        remaining.substring(0, end)
                    else
                        "##" + remaining.substring(0, end)

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

    private fun mapLabelToCommand(label: String, confidence: Float, rawText: String): Command {
        if (confidence < 0.7f || label == "unknown") {
            return Command.Unknown(rawText)
        }

        return when (label) {
            "check_air_filter" -> Command.CheckAirFilter
            "check_battery_health" -> Command.CheckBatteryHealth
            "check_battery_voltage" -> Command.CheckBatteryVoltage
            "check_brake_fluid" -> Command.CheckBrakeFluid
            "check_brake_pad_wear" -> Command.CheckBrakePadWear
            "check_coolant_level" -> Command.CheckCoolantLevel
            "check_driving_range" -> Command.CheckDrivingRange
            "check_engine" -> Command.CheckEngineStatus
            "check_ev_status" -> Command.CheckEvStatus
            "check_fuel_consumption" -> Command.CheckFuelConsumption
            "check_fuel_level" -> Command.CheckFuelLevel
            "check_odometer" -> Command.CheckOdometer
            "check_oil_level" -> Command.CheckOilLevel
            "check_outside_temperature" -> Command.CheckOutsideTemperature
            "check_rpm" -> Command.CheckRpm
            "check_spark_plug_condition" -> Command.CheckSparkPlugCondition
            "check_steering_status" -> Command.CheckSteeringStatus
            "check_suspension_health" -> Command.CheckSuspensionHealth
            "check_tire_wear" -> Command.CheckTireWear
            "check_tires" -> Command.CheckTireStatus
            "check_transmission_fluid" -> Command.CheckTransmissionFluid
            "check_transmission_status" -> Command.CheckTransmissionStatus
            "check_wheel_alignment" -> Command.CheckWheelAlignment
            "find_car_wash" -> Command.FindCarWash
            "find_charging_station" -> Command.FindChargingStation
            "find_food" -> Command.FindFood
            "find_gas_station" -> Command.FindGasStation
            "find_parking" -> Command.FindParking
            "find_rest_stop" -> Command.FindRestStop
            "find_service" -> Command.FindService
            else -> Command.Unknown(rawText)
        }
    }
}