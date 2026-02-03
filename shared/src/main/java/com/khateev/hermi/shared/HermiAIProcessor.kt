package com.khateev.hermi.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Singleton

@Singleton
class HermiAIProcessor(@param:ApplicationContext private val context: Context): AIProcessor {
    sealed class Command {
        object FindFood : Command()
        object VehicleStatus : Command()
        data class Unknown(val rawText: String) : Command()
    }

    sealed class HermiCommand {
        object SwitchToReal : Command()
        object SwitchToMock : Command()
        object GetVehicleStatus : Command()
    }

    fun parseCommand(input: String): Command {
        val text = input.lowercase()
        val foodKeywords = context.getString(R.string.ai_keywords_food).split(",")
        val vehicleKeywords = context.getString(R.string.ai_keywords_vehicle).split(",")

        return when {
            foodKeywords.any { text.contains(it) } ->
                Command.FindFood

            vehicleKeywords.any { text.contains(it) } ->
                Command.VehicleStatus

            text.contains("switch to real") || input.contains("real mode") ->
                HermiCommand.SwitchToReal
            text.contains("switch to mock") || input.contains("simulation mode") ->
                HermiCommand.SwitchToMock
            text.contains("check engine") || input.contains("status") ->
                HermiCommand.GetVehicleStatus

            else -> Command.Unknown(input)
        }
    }

    override fun processVehicleData(state: VhalParamsState): Flow<String> = flow {
        if (state.isFuelLow && state.range < 50f) {
            emit(context.getString(R.string.ai_fuel_low_warning, state.range))
        }

        if (state.coolantTemp > 105f) {
            emit(context.getString(R.string.ai_coolant_temp_warning, state.coolantTemp))
        }

        if (state.evBatteryLevel < 20f && !state.isEvChargePortConnected) {
            emit(context.getString(R.string.ai_ev_battery_low, state.evBatteryLevel))
        }

        if (state.isCriticallyLowTirePressure) {
            state.tirePressures.forEach { (area, pressure) ->
                if (pressure < 1.5f) { // Critical pressure
                    emit(context.getString(R.string.ai_tire_pressure_low, area))
                }
            }
        }
    }
}