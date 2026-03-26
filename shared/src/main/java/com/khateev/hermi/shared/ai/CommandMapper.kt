package com.khateev.hermi.shared.ai

import com.khateev.hermi.shared.model.Command

object CommandMapper {
    fun mapLabelToCommand(label: String, confidence: Float, rawText: String): Command {
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

    fun softmax(logits: FloatArray): FloatArray {
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
}
