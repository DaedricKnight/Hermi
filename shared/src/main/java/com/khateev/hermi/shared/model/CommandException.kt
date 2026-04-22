package com.khateev.hermi.shared.model

/**
 * Исключение, которое выбрасывается когда команда недоступна в текущей версии
 */
class CommandNotAvailableException(
    val command: Command,
    val requiresPro: Boolean = true
) : Exception(
    buildMessage(command, requiresPro)
) {
    companion object {
        private fun buildMessage(command: Command, requiresPro: Boolean): String {
            val commandName = CommandAccessControl.getCommandName(command)
            return if (requiresPro) {
                "Команда '$commandName' доступна только в Pro версии"
            } else {
                "Команда '$commandName' недоступна"
            }
        }
    }
}

/**
 * Исключение для попытки выполнить Pro команду без подписки
 */
class ProFeatureRequiredException(
    val commandName: String,
    override val message: String = "Для использования этой функции требуется Pro версия"
) : Exception(message)

