package com.khateev.hermi

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import com.khateev.hermi.shared.VhalParamsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.set
import kotlin.collections.toMutableMap

fun mapPropertyToState(
    value: CarPropertyValue<*>,
    currentState: VhalParamsState
): VhalParamsState {
    return when (value.propertyId) {
        // Main driving data
        VehiclePropertyIds.GEAR_SELECTION -> currentState.copy(gear = value.value as Int)
        VehiclePropertyIds.PARKING_BRAKE_ON -> currentState.copy(isParkingBrakeOn = value.value as Boolean)
        VehiclePropertyIds.FUEL_LEVEL -> currentState.copy(fuelLevel = value.value as Float)
        VehiclePropertyIds.FUEL_LEVEL_LOW -> currentState.copy(isFuelLow = value.value as Boolean)
        VehiclePropertyIds.ENGINE_OIL_LEVEL -> currentState.copy(oilLevel = value.value as Float)
        VehiclePropertyIds.RANGE_REMAINING -> currentState.copy(range = value.value as Float)
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> currentState.copy(outsideTemp = value.value as Float)

        // --- ENGINE & MAINTENANCE ---
        VehiclePropertyIds.ENGINE_COOLANT_TEMP -> currentState.copy(coolantTemp = value.value as Float)
        VehiclePropertyIds.ENGINE_OIL_TEMP -> currentState.copy(oilTemp = value.value as Float)
        VehiclePropertyIds.ENGINE_RPM -> currentState.copy(rpm = (value.value as Float).toInt())
        VehiclePropertyIds.IGNITION_STATE -> currentState.copy(ignitionState = value.value as Int)
        VehiclePropertyIds.PERF_ODOMETER -> currentState.copy(odometer = value.value as Float)

        // --- LIGHTS ---
        VehiclePropertyIds.TURN_SIGNAL_STATE -> currentState.copy(turnSignal = value.value as Int)
        VehiclePropertyIds.HEADLIGHTS_STATE -> currentState.copy(headLightState = value.value as Int)
        VehiclePropertyIds.HIGH_BEAM_LIGHTS_STATE -> currentState.copy(highBeamState = value.value as Int)
        VehiclePropertyIds.NIGHT_MODE -> currentState.copy(nighMode = value.value as Boolean)

        // --- EV CHARGE ---
        VehiclePropertyIds.EV_CHARGE_STATE -> currentState.copy(evCharge = value.value as Int)
        VehiclePropertyIds.EV_CHARGE_TIME_REMAINING -> currentState.copy(evChargeTime = value.value as Int)
        VehiclePropertyIds.EV_REGENERATIVE_BRAKING_STATE -> currentState.copy(isEvRecuperate = value.value as Int)

        // --- SAFETY SYSTEMS ---
        VehiclePropertyIds.ABS_ACTIVE -> currentState.copy(isAbsActive = value.value as Boolean)
        VehiclePropertyIds.TRACTION_CONTROL_ACTIVE -> currentState.copy(isTractionControlActive = value.value as Boolean)
        VehiclePropertyIds.TIRE_PRESSURE -> {
            val updatedPressures = currentState.tirePressures.toMutableMap()
            updatedPressures[value.areaId] = value.value as Float
            currentState.copy(tirePressures = updatedPressures)
        }
        VehiclePropertyIds.CRITICALLY_LOW_TIRE_PRESSURE -> currentState.copy(isCriticallyLowTirePressure = value.value as Boolean)

        // --- ELECTRICAL & BODY ---
        VehiclePropertyIds.EV_BATTERY_LEVEL -> currentState.copy(evBatteryLevel = value.value as Float)
        VehiclePropertyIds.EV_BATTERY_DISPLAY_UNITS -> currentState.copy(evBatteryUnits = value.value as String)
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> currentState.copy(isEvChargePortConnected = value.value as Boolean)
        VehiclePropertyIds.DOOR_LOCK ->
            // Door lock can be per-door (areaId).
            // For a general OBD state, we might check if ANY door is unlocked
            currentState.copy(isDoorLocked = value.value as Boolean)

        // --- DIAGNOSTICS ---
        VehiclePropertyIds.OBD2_LIVE_FRAME -> currentState.copy(obdData = parseObdFrame(value))
        VehiclePropertyIds.OBD2_FREEZE_FRAME -> currentState.copy(obdFreezeFrame = value.value.toString())

        else -> currentState
    }
}

fun readStaticValue(propertyManager: CarPropertyManager?, id: Int, currentState: MutableStateFlow<VhalParamsState>) {
    val manager = propertyManager ?: return
    try {
        val value = manager.getProperty<Any>(id, 0).value
        currentState.update { currentState ->
            when (id) {
                VehiclePropertyIds.INFO_VIN -> currentState.copy(vin = value as String)
                VehiclePropertyIds.INFO_FUEL_TYPE -> currentState.copy(fuelType = value as Array<Int>)
                VehiclePropertyIds.INFO_MAKE -> currentState.copy(make = value as String)
                VehiclePropertyIds.INFO_MODEL -> currentState.copy(model = value as String)
                VehiclePropertyIds.INFO_MODEL_YEAR -> currentState.copy(modelYear = value as Int)
                VehiclePropertyIds.INFO_FUEL_CAPACITY -> currentState.copy(fuelCapacity = value as Float)
                VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> currentState.copy(evBatteryCapacity = value as Float)
                VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS -> currentState.copy(tempUnit = value as String)
                else -> currentState
            }
        }
    } catch (e: Exception) {
        Log.w("VHAL", "Could not read static property $id, error: $e")
    }
}

private fun parseObdFrame(value: CarPropertyValue<*>): String {
    // In AAOS, the OBD frame often arrives as an Integer or Byte array
    val rawData = value.value ?: return "No Data"

    // This is simplified logic. In a production environment, you would use
    // a specialized API to extract Diagnostic Trouble Codes (DTC).
    return try {
        // Example: extracting Check Engine status and fault codes.
        // In practice, you will need the documentation for your specific VHAL implementation.
        "System Status: OK | DTCs: None"
    } catch (e: Exception) {
        "Parsing Error"
    }
}