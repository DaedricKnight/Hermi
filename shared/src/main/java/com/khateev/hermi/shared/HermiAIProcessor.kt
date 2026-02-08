package com.khateev.hermi.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Singleton

@Singleton
class HermiAIProcessor(@param:ApplicationContext private val context: Context): AIProcessor {
    sealed class Command {
        // Navigation and search
        object FindFood : Command()
        object FindGasStation : Command()
        object FindChargingStation : Command()
        object FindParking : Command()

        // Car status
        object VehicleStatus : Command()
        object CheckEngine : Command()
        object CheckTires : Command()
        object CheckFuelLevel : Command()
        object CheckBatteryLevel : Command()

        // Management
        object LockDoors : Command()
        object UnlockDoors : Command()
        object TurnOnLights : Command()
        object TurnOffLights : Command()

        // Info
        object GetRange : Command()
        object GetOdometer : Command()
        object GetSpeed : Command()
        object GetOutsideTemp : Command()

        // Mode
        object SwitchToReal : Command()
        object SwitchToMock : Command()

        data class Unknown(val rawText: String) : Command()
    }

    sealed class HermiCommand {
        object SwitchToReal : Command()
        object SwitchToMock : Command()
    }

    fun parseCommand(input: String): Command {
        val text = input.lowercase()

        return when {
            // Navigation
            listOf("food", "restaurant", "cafe", "eat", "hungry").any { text.contains(it) } ->
                Command.FindFood

            listOf("gas", "fuel", "gas station", "refuel", "petrol").any { text.contains(it) } ->
                Command.FindGasStation

            listOf("charging", "charge", "charging station", "electric").any { text.contains(it) } ->
                Command.FindChargingStation

            listOf("parking", "park", "parking lot").any { text.contains(it) } ->
                Command.FindParking

            // Checks
            listOf("engine", "check engine", "problem", "issue").any { text.contains(it) } ->
                Command.CheckEngine

            listOf("tire", "pressure", "tires", "tire pressure").any { text.contains(it) } ->
                Command.CheckTires

            listOf("fuel level", "fuel", "gas level", "how much fuel").any { text.contains(it) } ->
                Command.CheckFuelLevel

            listOf("battery", "charge", "battery level", "ev battery").any { text.contains(it) } ->
                Command.CheckBatteryLevel

            // Control
            listOf("lock doors", "lock", "secure").any { text.contains(it) } ->
                Command.LockDoors

            listOf("unlock doors", "unlock", "open doors").any { text.contains(it) } ->
                Command.UnlockDoors

            listOf("lights on", "turn on lights", "headlights on").any { text.contains(it) } ->
                Command.TurnOnLights

            listOf("lights off", "turn off lights", "headlights off").any { text.contains(it) } ->
                Command.TurnOffLights

            // Information
            listOf("range", "how far", "distance left").any { text.contains(it) } ->
                Command.GetRange

            listOf("odometer", "mileage", "distance traveled").any { text.contains(it) } ->
                Command.GetOdometer

            listOf("speed", "how fast", "current speed").any { text.contains(it) } ->
                Command.GetSpeed

            listOf("temperature", "outside temp", "weather").any { text.contains(it) } ->
                Command.GetOutsideTemp

            // Status
            listOf("status", "vehicle status", "car status", "condition").any { text.contains(it) } ->
                Command.VehicleStatus

            // Modes
            listOf("real mode", "switch to real", "actual data").any { text.contains(it) } ->
                Command.SwitchToReal

            listOf("mock", "test mode", "simulation", "demo").any { text.contains(it) } ->
                Command.SwitchToMock

            else -> Command.Unknown(input)
        }
    }

    override fun processVehicleData(state: VhalParamsState): Flow<String> = flow {
        // Critical warnings
        if (state.isFuelLow && state.range < 50f) {
            emit("Low fuel level! Range remaining: ${state.range.toInt()} km")
        }

        if (state.coolantTemp > 105f) {
            emit("WARNING! High coolant temperature: ${state.coolantTemp}°C")
        }

        if (state.evBatteryLevel < 20f && !state.isEvChargePortConnected) {
            emit("Low battery charge: ${state.evBatteryLevel.toInt()}%. I recommend finding a charging station")
        }

        if (state.isCriticallyLowTirePressure) {
            emit("CRITICAL tire pressure! Inspection required")
        }

        if (state.isCheckEngineOn) {
            val dtcInfo = if (state.activeDTCs.isNotEmpty()) {
                " Error codes: ${state.activeDTCs.joinToString()}"
            } else ""
            emit("Check Engine light is on.$dtcInfo")
        }

        // Reminders
        if (!state.isDoorLocked && state.ignitionState == 0) {
            emit("Doors are unlocked")
        }

        if (state.isParkingBrakeOn && state.speed > 5f) {
            emit("Parking brake is engaged!")
        }
    }
}