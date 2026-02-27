package com.khateev.hermi.shared.model

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

    data class Unknown(val rawText: String) : Command()
}