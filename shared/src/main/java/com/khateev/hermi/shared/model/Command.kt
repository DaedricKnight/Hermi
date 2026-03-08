package com.khateev.hermi.shared.model

sealed class Command {
    // ==================== FREE VERSION (5 commands) ====================
    // Navigation — local, no internet needed (OSM / cached data)

    object FindFood : Command()
    object FindGasStation : Command()
    object FindChargingStation : Command()
    object FindParking : Command()
    object FindRestStop : Command()          // rest areas, roadside stops


    // ==================== PRO VERSION (24 commands) ====================

    // ===== ENGINE & POWERTRAIN (4 commands) =====
    // CheckCoolantTemperature removed — coolantTemp already included in CheckEngineStatus
    // CheckSparkPlugCondition moved under maintenance (conceptually minor sensor)

    object CheckEngineStatus : Command()         // isCheckEngineOn, activeDTCs, coolantTemp
    object CheckOilLevel : Command()             // oilLevel
    object CheckTransmissionStatus : Command()   // transmissionStatus
    object CheckBrakePadWear : Command()         // brakePadWear
    object CheckSparkPlugCondition : Command()   // sparkPlugCondition
    object CheckRpm : Command()   // sparkPlugCondition


    // ===== FUEL & BATTERY (5 commands) =====
    // CheckEvChargeStatus + CheckElectricRange merged into CheckEvStatus
    // Both read evBatteryLevel — no reason to split

    object CheckFuelLevel : Command()            // fuelLevel, isFuelLow
    object CheckFuelConsumption : Command()      // fuelConsumption, avgMpg
    object CheckBatteryVoltage : Command()       // batteryVoltage
    object CheckBatteryHealth : Command()        // batteryHealthStatus, batteryTemperature
    object CheckEvStatus : Command()             // evBatteryLevel, isEvChargePortConnected,
    // evChargeTime, estimatedEvRange


    // ===== TIRES & SUSPENSION (5 commands) =====

    object CheckTireStatus : Command()           // isCriticallyLowTirePressure, tirePressures
    object CheckTireWear : Command()             // tireWear
    object CheckSuspensionHealth : Command()     // suspensionStatus
    object CheckWheelAlignment : Command()       // wheelAlignment
    object CheckSteeringStatus : Command()       // steeringStatus


    // ===== FLUIDS & FILTERS (4 commands) =====

    object CheckTransmissionFluid : Command()    // transmissionFluidStatus
    object CheckBrakeFluid : Command()           // brakeFluidStatus
    object CheckCoolantLevel : Command()         // coolantLevel
    object CheckAirFilter : Command()            // airFilterStatus, cabinFilterStatus


    // ===== DRIVING STATS (3 commands) =====
    object CheckOdometer : Command()             // odometer
    object CheckDrivingRange : Command()         // range (fuel/EV depending on vehicle type)


    // ===== ENVIRONMENTAL (1 command) =====
    // CheckAmbientConditions removed — full duplicate of CheckOutsideTemperature (same field: outsideTemp)

    object CheckOutsideTemperature : Command()   // outsideTemp, tempUnit


    // ===== FIND (PRO EXTENDED) (2 commands) =====
    // Smart find commands triggered contextually (e.g. after active DTCs → FindService)

    object FindService : Command()               // auto repair / service station
    object FindCarWash : Command()               // car wash


    // ==================== FALLBACK ====================

    data class Unknown(val rawText: String) : Command()
}