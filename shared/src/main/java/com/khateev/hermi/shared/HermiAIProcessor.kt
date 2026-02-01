package com.khateev.hermi.shared

import android.content.Context

class HermiAIProcessor(private val context: Context) {
    sealed class Command {
        object FindFood : Command()
        object VehicleStatus : Command()
        data class Unknown(val rawText: String) : Command()
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

            else -> Command.Unknown(input)
        }
    }
}