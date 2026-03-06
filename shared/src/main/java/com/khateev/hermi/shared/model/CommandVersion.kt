package com.khateev.hermi.shared.model

/**
 * Определяет в какой версии приложения доступна команда
 */
enum class CommandVersion {
    FREE,    // Доступна в бесплатной версии
    PRO      // Доступна только в платной версии (Pro)
}

/**
 * Класс для управления доступностью команд в зависимости от версии
 */
object CommandAccessControl {

    /**
     * Карта команд и их версионности
     */
    private val commandVersionMap = mapOf(
        // === FREE версия (4 команды) ===
        "FindFood" to CommandVersion.FREE,
        "FindGasStation" to CommandVersion.FREE,
        "FindChargingStation" to CommandVersion.FREE,
        "FindParking" to CommandVersion.FREE,

        // === PRO версия (13 команд) ===
        "VehicleStatus" to CommandVersion.PRO,
        "CheckEngine" to CommandVersion.PRO,
        "CheckTires" to CommandVersion.PRO,
        "CheckFuelLevel" to CommandVersion.PRO,
        "CheckBatteryLevel" to CommandVersion.PRO,
        "LockDoors" to CommandVersion.PRO,
        "UnlockDoors" to CommandVersion.PRO,
        "TurnOnLights" to CommandVersion.PRO,
        "TurnOffLights" to CommandVersion.PRO,
        "GetRange" to CommandVersion.PRO,
        "GetOdometer" to CommandVersion.PRO,
        "GetSpeed" to CommandVersion.PRO,
        "GetOutsideTemp" to CommandVersion.PRO
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
            // Navigation and search
            Command.FindFood -> "FindFood"
            Command.FindGasStation -> "FindGasStation"
            Command.FindChargingStation -> "FindChargingStation"
            Command.FindParking -> "FindParking"

            // Car status
            Command.VehicleStatus -> "VehicleStatus"
            Command.CheckEngine -> "CheckEngine"
            Command.CheckTires -> "CheckTires"
            Command.CheckFuelLevel -> "CheckFuelLevel"
            Command.CheckBatteryLevel -> "CheckBatteryLevel"

            // Management
            Command.LockDoors -> "LockDoors"
            Command.UnlockDoors -> "UnlockDoors"
            Command.TurnOnLights -> "TurnOnLights"
            Command.TurnOffLights -> "TurnOffLights"

            // Info
            Command.GetRange -> "GetRange"
            Command.GetOdometer -> "GetOdometer"
            Command.GetSpeed -> "GetSpeed"
            Command.GetOutsideTemp -> "GetOutsideTemp"

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

