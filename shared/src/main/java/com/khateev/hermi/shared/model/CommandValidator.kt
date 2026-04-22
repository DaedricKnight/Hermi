package com.khateev.hermi.shared.model

/**
 * Utility for working with command availability
 */
object CommandValidator {

    /**
     * Checks command availability and throws an exception if unavailable
     * @param command Command to check
     * @param isPro Whether the user is Pro
     * @throws CommandNotAvailableException if the command is unavailable
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
     * Checks command availability safely (without exceptions)
     * @return Result with availability information
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
     * Gets command availability information
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
 * Result of command availability check
 */
sealed class CommandAccessResult {
    object Available : CommandAccessResult()
    object RequiresPro : CommandAccessResult()
    object Unavailable : CommandAccessResult()
}

/**
 * Command information
 */
data class CommandInfo(
    val name: String,
    val version: CommandVersion?,
    val requiresPro: Boolean
)

/**
 * Extension function to check availability
 */
fun Command.isAvailable(isPro: Boolean): Boolean {
    return CommandAccessControl.isCommandAvailable(this, isPro)
}

/**
 * Extension function to get command information
 */
fun Command.getInfo(): CommandInfo {
    return CommandValidator.getCommandInfo(this)
}

/**
 * Extension function to check if Pro version is required
 */
fun Command.requiresPro(): Boolean {
    return CommandAccessControl.requiresPro(this)
}

