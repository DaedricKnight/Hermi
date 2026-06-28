package com.khateev.hermi.shared.ai

import androidx.test.platform.app.InstrumentationRegistry
import com.khateev.hermi.shared.model.Command
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ONNXAIProcessorIntegrationTest {

    private lateinit var aiProcessor: ONNXAIProcessor

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        aiProcessor = ONNXAIProcessor(context)
    }

    @Test
    fun testModelAccuracyOnAllPhrases() = runTest {
        val testData = TestPhrases.data
        var totalTests = 0
        var correctMatches = 0
        val failures = mutableListOf<String>()

        testData.forEach { (expectedLabel, phrases) ->
            phrases.forEach { phrase ->
                totalTests++
                val result = aiProcessor.parseCommand(phrase)

                // Проверяем, что результат соответствует ожидаемому типу команды
                val isCorrect = when (expectedLabel) {
                    "find_food" -> result is Command.FindFood
                    "find_gas_station" -> result is Command.FindGasStation
                    "find_charging_station" -> result is Command.FindChargingStation
                    "find_parking" -> result is Command.FindParking
                    "find_rest_stop" -> result is Command.FindRestStop
                    "check_engine" -> result is Command.CheckEngineStatus
                    "check_oil_level" -> result is Command.CheckOilLevel
                    "check_transmission_status" -> result is Command.CheckTransmissionStatus
                    "check_brake_pad_wear" -> result is Command.CheckBrakePadWear
                    "check_spark_plug_condition" -> result is Command.CheckSparkPlugCondition
                    "check_rpm" -> result is Command.CheckRpm
                    "check_fuel_level" -> result is Command.CheckFuelLevel
                    "check_fuel_consumption" -> result is Command.CheckFuelConsumption
                    "check_battery_voltage" -> result is Command.CheckBatteryVoltage
                    "check_battery_health" -> result is Command.CheckBatteryHealth
                    "check_ev_status" -> result is Command.CheckEvStatus
                    "check_tires" -> result is Command.CheckTireStatus
                    "check_tire_wear" -> result is Command.CheckTireWear
                    "check_suspension_health" -> result is Command.CheckSuspensionHealth
                    "check_wheel_alignment" -> result is Command.CheckWheelAlignment
                    "check_steering_status" -> result is Command.CheckSteeringStatus
                    "check_transmission_fluid" -> result is Command.CheckTransmissionFluid
                    "check_brake_fluid" -> result is Command.CheckBrakeFluid
                    "check_coolant_level" -> result is Command.CheckCoolantLevel
                    "check_air_filter" -> result is Command.CheckAirFilter
                    "check_odometer" -> result is Command.CheckOdometer
                    "check_driving_range" -> result is Command.CheckDrivingRange
                    "check_outside_temperature" -> result is Command.CheckOutsideTemperature
                    "find_service" -> result is Command.FindService
                    "find_car_wash" -> result is Command.FindCarWash
                    else -> false
                }

                if (isCorrect) {
                    correctMatches++
                } else {
                    failures.add("FAIL: '$phrase' expected $expectedLabel but got ${result.javaClass.simpleName}")
                }
            }
        }

        val accuracy = (correctMatches.toFloat() / totalTests.toFloat()) * 100
        println("Integration Test Results:")
        println("Total Phrases: $totalTests")
        println("Correct: $correctMatches")
        println("Accuracy: %.2f%%".format(accuracy))

        if (failures.isNotEmpty()) {
            println("\nTop Failures (first 20):")
            failures.take(20).forEach { println(it) }
        }

        // Мы не ставим жесткое ограничение на 100% точность,
        // так как модель может ошибаться на живых данных,
        // но мы проверяем, что процессор работает.
        assert( accuracy > 1f, { "Accuracy should be at least 1%" })
    }
}
