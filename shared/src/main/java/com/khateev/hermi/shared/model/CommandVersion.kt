package com.khateev.hermi.shared.model

/**
 * Defines in which version of the application the command is available
 */
enum class CommandVersion {
    FREE,    // Available in the free version
    PRO      // Available only in the paid version (Pro)
}

/**
 * Class for managing command availability depending on the version
 */
object CommandAccessControl {

    /**
     * Map of commands and their versioning
     */
    private val commandVersionMap = mapOf(
        // === FREE version (5 commands) ===
        // Navigation — local, no internet needed
        "FindFood" to CommandVersion.FREE,
        "FindGasStation" to CommandVersion.FREE,
        "FindChargingStation" to CommandVersion.FREE,
        "FindParking" to CommandVersion.FREE,
        "FindRestStop" to CommandVersion.FREE,

        // === PRO версия (24 команды) ===
        // Engine & Powertrain (5)
        "CheckEngineStatus" to CommandVersion.PRO,
        "CheckOilLevel" to CommandVersion.PRO,
        "CheckTransmissionStatus" to CommandVersion.PRO,
        "CheckBrakePadWear" to CommandVersion.PRO,
        "CheckSparkPlugCondition" to CommandVersion.PRO,
        "CheckRpm" to CommandVersion.PRO,

        // Fuel & Battery (5)
        "CheckFuelLevel" to CommandVersion.PRO,
        "CheckFuelConsumption" to CommandVersion.PRO,
        "CheckBatteryVoltage" to CommandVersion.PRO,
        "CheckBatteryHealth" to CommandVersion.PRO,
        "CheckEvStatus" to CommandVersion.PRO,

        // Tires & Suspension (5)
        "CheckTireStatus" to CommandVersion.PRO,
        "CheckTireWear" to CommandVersion.PRO,
        "CheckSuspensionHealth" to CommandVersion.PRO,
        "CheckWheelAlignment" to CommandVersion.PRO,
        "CheckSteeringStatus" to CommandVersion.PRO,

        // Fluids & Filters (4)
        "CheckTransmissionFluid" to CommandVersion.PRO,
        "CheckBrakeFluid" to CommandVersion.PRO,
        "CheckCoolantLevel" to CommandVersion.PRO,
        "CheckAirFilter" to CommandVersion.PRO,

        // Driving Stats (2)
        "CheckOdometer" to CommandVersion.PRO,
        "CheckDrivingRange" to CommandVersion.PRO,

        // Environmental (1)
        "CheckOutsideTemperature" to CommandVersion.PRO,

        // Find (PRO Extended) (2)
        "FindService" to CommandVersion.PRO,
        "FindCarWash" to CommandVersion.PRO,

        // Fallback
        "Unknown" to CommandVersion.FREE
    )

    /**
     * Проверяет, доступна ли команда для текущей версии приложения
     * @param command Команда для проверки
     * @param isPro Является ли пользователь Pro (платным) пользователем
     * @return true если команда доступна, false если нет
     */
    fun isCommandAvailable(command: Command, isPro: Boolean): Boolean {
        val commandName = getCommandName(command)
        val requiredVersion = commandVersionMap[commandName] ?: return false

        return when {
            requiredVersion == CommandVersion.FREE -> true  // FREE команды всегда доступны
            requiredVersion == CommandVersion.PRO && isPro -> true  // PRO команды только для Pro
            else -> false
        }
    }

    /**
     * Проверяет, требуется ли Pro версия для команды
     * @param command Команда для проверки
     * @return true если команда требует Pro версию
     */
    fun requiresPro(command: Command): Boolean {
        val commandName = getCommandName(command)
        return commandVersionMap[commandName] == CommandVersion.PRO
    }

    /**
     * Получает название команды из sealed class
     */
    fun getCommandName(command: Command): String {
        return when (command) {
            // Navigation and search (FREE)
            Command.FindFood -> "FindFood"
            Command.FindGasStation -> "FindGasStation"
            Command.FindChargingStation -> "FindChargingStation"
            Command.FindParking -> "FindParking"
            Command.FindRestStop -> "FindRestStop"

            // Engine & Powertrain (PRO)
            Command.CheckEngineStatus -> "CheckEngineStatus"
            Command.CheckOilLevel -> "CheckOilLevel"
            Command.CheckTransmissionStatus -> "CheckTransmissionStatus"
            Command.CheckBrakePadWear -> "CheckBrakePadWear"
            Command.CheckSparkPlugCondition -> "CheckSparkPlugCondition"
            Command.CheckRpm -> "CheckRpm"

            // Fuel & Battery (PRO)
            Command.CheckFuelLevel -> "CheckFuelLevel"
            Command.CheckFuelConsumption -> "CheckFuelConsumption"
            Command.CheckBatteryVoltage -> "CheckBatteryVoltage"
            Command.CheckBatteryHealth -> "CheckBatteryHealth"
            Command.CheckEvStatus -> "CheckEvStatus"

            // Tires & Suspension (PRO)
            Command.CheckTireStatus -> "CheckTireStatus"
            Command.CheckTireWear -> "CheckTireWear"
            Command.CheckSuspensionHealth -> "CheckSuspensionHealth"
            Command.CheckWheelAlignment -> "CheckWheelAlignment"
            Command.CheckSteeringStatus -> "CheckSteeringStatus"

            // Fluids & Filters (PRO)
            Command.CheckTransmissionFluid -> "CheckTransmissionFluid"
            Command.CheckBrakeFluid -> "CheckBrakeFluid"
            Command.CheckCoolantLevel -> "CheckCoolantLevel"
            Command.CheckAirFilter -> "CheckAirFilter"

            // Driving Stats (PRO)
            Command.CheckOdometer -> "CheckOdometer"
            Command.CheckDrivingRange -> "CheckDrivingRange"

            // Environmental (PRO)
            Command.CheckOutsideTemperature -> "CheckOutsideTemperature"

            // Find (PRO Extended)
            Command.FindService -> "FindService"
            Command.FindCarWash -> "FindCarWash"

            is Command.Unknown -> "Unknown"
        }
    }

    /**
     * Возвращает список всех команд для конкретной версии
     */
    fun getAvailableCommands(isPro: Boolean): List<String> {
        return commandVersionMap.filter { (_, version) ->
            version == CommandVersion.FREE || (version == CommandVersion.PRO && isPro)
        }.keys.toList()
    }

    /**
     * Возвращает тип версии для команды
     */
    fun getCommandVersion(command: Command): CommandVersion? {
        val commandName = getCommandName(command)
        return commandVersionMap[commandName]
    }
}
