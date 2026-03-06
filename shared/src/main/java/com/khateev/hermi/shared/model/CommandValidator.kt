package com.khateev.hermi.shared.model

/**
 * Утилита для работы с доступностью команд
 */
object CommandValidator {

    /**
     * Проверяет доступность команды и выбрасывает исключение если недоступна
     * @param command Команда для проверки
     * @param isPro Является ли пользователь Pro
     * @throws CommandNotAvailableException если команда недоступна
     */
    @Throws(CommandNotAvailableException::class)
    fun validateCommandAccess(command: Command, isPro: Boolean) {
        if (!CommandAccessControl.isCommandAvailable(command, isPro)) {
            throw CommandNotAvailableException(
                command = command,
                requiresPro = CommandAccessControl.requiresPro(command)
            )
        }
    }

    /**
     * Проверяет доступность команды безопасно (без исключений)
     * @return Result с информацией о доступности
     */
    fun checkCommandAccess(command: Command, isPro: Boolean): CommandAccessResult {
        return if (CommandAccessControl.isCommandAvailable(command, isPro)) {
            CommandAccessResult.Available
        } else {
            if (CommandAccessControl.requiresPro(command)) {
                CommandAccessResult.RequiresPro
            } else {
                CommandAccessResult.Unavailable
            }
        }
    }

    /**
     * Получает информацию о доступности команды
     */
    fun getCommandInfo(command: Command): CommandInfo {
        val version = CommandAccessControl.getCommandVersion(command)
        return CommandInfo(
            name = CommandAccessControl.getCommandName(command),
            version = version,
            requiresPro = version == CommandVersion.PRO
        )
    }
}

/**
 * Результат проверки доступности команды
 */
sealed class CommandAccessResult {
    object Available : CommandAccessResult()
    object RequiresPro : CommandAccessResult()
    object Unavailable : CommandAccessResult()
}

/**
 * Информация о команде
 */
data class CommandInfo(
    val name: String,
    val version: CommandVersion?,
    val requiresPro: Boolean
)

/**
 * Extension функция для проверки доступности
 */
fun Command.isAvailable(isPro: Boolean): Boolean {
    return CommandAccessControl.isCommandAvailable(this, isPro)
}

/**
 * Extension функция для получения информации о команде
 */
fun Command.getInfo(): CommandInfo {
    return CommandValidator.getCommandInfo(this)
}

/**
 * Extension функция для проверки требования Pro версии
 */
fun Command.requiresPro(): Boolean {
    return CommandAccessControl.requiresPro(this)
}

