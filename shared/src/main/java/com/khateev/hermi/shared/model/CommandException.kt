package com.khateev.hermi.shared.model

/**
 * Thrown when a command is not available in the current app version
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
                "Command '$commandName' is available in the Pro version only"
            } else {
                "Command '$commandName' is unavailable"
            }
        }
    }
}

/**
 * Thrown when a Pro command is invoked without a subscription
 */
class ProFeatureRequiredException(
    val commandName: String,
    override val message: String = "This feature requires the Pro version"
) : Exception(message)

