package com.khateev.hermi.shared

data class VhalParamsState(
    val gear: Int = 0,
    val isParkingBrakeOn: Boolean = false,
    val odometer: Float = 0f,
    val rpm: Int = 0,
    val range: Float = 0f,
    val speed: Float = 0f,

    val fuelLevel: Float = 0f,
    val isFuelLow: Boolean = false,
    val evBatteryLevel: Float = 0f,
    val isEvChargePortConnected: Boolean = false,

    val coolantTemp: Float = 0f,
    val oilLevel: Float = 0f,
    val oilTemp: Float = 0f,
    val ignitionState: Int = 0,

    val tirePressures: Map<Int, Float> = emptyMap(),
    val isCriticallyLowTirePressure: Boolean = false,
    val isAbsActive: Boolean = false,
    val isTractionControlActive: Boolean = false,
    val isDoorLocked: Boolean = true,

    val isCheckEngineOn: Boolean = false,
    val activeDTCs: List<String> = emptyList(),
    val obdData: String = "",
    val obdFreezeFrame: String = "",           // Data snapshot on fault
    val fuelCapacity: Float = 0f,
    val fuelCapacityType: String = "",
    val outsideTemp: Float = 0f,
    val turnSignal: Int = 0,
    val headLightState: Int = 0,
    val highBeamState: Int = 0,
    val nighMode: Boolean = false,
    val evCharge: Int = 0,
    val evChargeTime: Int = 0,
    val isEvRecuperate: Int = 0,
    val vin: String = "",
    val fuelType: Array<Int> = emptyArray(),
    val make: String = "",
    val model: String = "",
    val modelYear: Int = 0,
    val evBatteryCapacity: Float = 0f,
    val evBatteryUnits: String = "",
    val tempUnit: String = ""
) {
    companion object {
        val initial = VhalParamsState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VhalParamsState

        if (gear != other.gear) return false
        if (isParkingBrakeOn != other.isParkingBrakeOn) return false
        if (odometer != other.odometer) return false
        if (rpm != other.rpm) return false
        if (range != other.range) return false
        if (fuelLevel != other.fuelLevel) return false
        if (isFuelLow != other.isFuelLow) return false
        if (evBatteryLevel != other.evBatteryLevel) return false
        if (isEvChargePortConnected != other.isEvChargePortConnected) return false
        if (coolantTemp != other.coolantTemp) return false
        if (oilLevel != other.oilLevel) return false
        if (oilTemp != other.oilTemp) return false
        if (ignitionState != other.ignitionState) return false
        if (isCriticallyLowTirePressure != other.isCriticallyLowTirePressure) return false
        if (isAbsActive != other.isAbsActive) return false
        if (isTractionControlActive != other.isTractionControlActive) return false
        if (isDoorLocked != other.isDoorLocked) return false
        if (isCheckEngineOn != other.isCheckEngineOn) return false
        if (fuelCapacity != other.fuelCapacity) return false
        if (outsideTemp != other.outsideTemp) return false
        if (turnSignal != other.turnSignal) return false
        if (headLightState != other.headLightState) return false
        if (highBeamState != other.highBeamState) return false
        if (nighMode != other.nighMode) return false
        if (evCharge != other.evCharge) return false
        if (evChargeTime != other.evChargeTime) return false
        if (isEvRecuperate != other.isEvRecuperate) return false
        if (modelYear != other.modelYear) return false
        if (evBatteryCapacity != other.evBatteryCapacity) return false
        if (tirePressures != other.tirePressures) return false
        if (activeDTCs != other.activeDTCs) return false
        if (obdData != other.obdData) return false
        if (obdFreezeFrame != other.obdFreezeFrame) return false
        if (fuelCapacityType != other.fuelCapacityType) return false
        if (vin != other.vin) return false
        if (!fuelType.contentEquals(other.fuelType)) return false
        if (make != other.make) return false
        if (model != other.model) return false
        if (evBatteryUnits != other.evBatteryUnits) return false
        if (tempUnit != other.tempUnit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gear
        result = 31 * result + isParkingBrakeOn.hashCode()
        result = 31 * result + odometer.hashCode()
        result = 31 * result + rpm
        result = 31 * result + range.hashCode()
        result = 31 * result + fuelLevel.hashCode()
        result = 31 * result + isFuelLow.hashCode()
        result = 31 * result + evBatteryLevel.hashCode()
        result = 31 * result + isEvChargePortConnected.hashCode()
        result = 31 * result + coolantTemp.hashCode()
        result = 31 * result + oilLevel.hashCode()
        result = 31 * result + oilTemp.hashCode()
        result = 31 * result + ignitionState
        result = 31 * result + isCriticallyLowTirePressure.hashCode()
        result = 31 * result + isAbsActive.hashCode()
        result = 31 * result + isTractionControlActive.hashCode()
        result = 31 * result + isDoorLocked.hashCode()
        result = 31 * result + isCheckEngineOn.hashCode()
        result = 31 * result + fuelCapacity.hashCode()
        result = 31 * result + outsideTemp.hashCode()
        result = 31 * result + turnSignal
        result = 31 * result + headLightState
        result = 31 * result + highBeamState
        result = 31 * result + nighMode.hashCode()
        result = 31 * result + evCharge
        result = 31 * result + evChargeTime
        result = 31 * result + isEvRecuperate
        result = 31 * result + modelYear
        result = 31 * result + evBatteryCapacity.hashCode()
        result = 31 * result + tirePressures.hashCode()
        result = 31 * result + activeDTCs.hashCode()
        result = 31 * result + obdData.hashCode()
        result = 31 * result + obdFreezeFrame.hashCode()
        result = 31 * result + fuelCapacityType.hashCode()
        result = 31 * result + vin.hashCode()
        result = 31 * result + fuelType.contentHashCode()
        result = 31 * result + make.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + evBatteryUnits.hashCode()
        result = 31 * result + tempUnit.hashCode()
        return result
    }

}
