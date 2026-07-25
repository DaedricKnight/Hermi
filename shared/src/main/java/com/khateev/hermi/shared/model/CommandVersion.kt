package com.khateev.hermi.shared.model

/**
 * Defines in which version of the application the command is available
 */
enum class CommandVersion {
    FREE,    // Available in the free version
    PRO      // Available only in the paid version (Pro)
}

/**
 * Command availability by app version.
 *
 * The tier of every command lives in [CommandCatalog] alongside the model label
 * it comes from, so adding an intent to the model is the only place tiers are
 * declared. [Command.Unknown] is always available — it is how the assistant says
 * "I didn't get that".
 */
object CommandAccessControl {

    fun isCommandAvailable(command: Command, isPro: Boolean): Boolean {
        if (command is Command.Unknown) return true
        return when (CommandCatalog.specFor(command)?.version) {
            CommandVersion.FREE -> true
            CommandVersion.PRO -> isPro
            null -> false
        }
    }

    fun requiresPro(command: Command): Boolean =
        CommandCatalog.specFor(command)?.version == CommandVersion.PRO

    /** Human-readable name, e.g. "Lock doors" — shown in dialogs and the list. */
    fun getCommandName(command: Command): String =
        CommandCatalog.specFor(command)?.title ?: "Unknown"

    /** Titles of the commands the given tier may run. */
    fun getAvailableCommands(isPro: Boolean): List<String> =
        CommandCatalog.specs
            .filter { it.version == CommandVersion.FREE || isPro }
            .map { it.title }

    fun getCommandVersion(command: Command): CommandVersion? =
        if (command is Command.Unknown) CommandVersion.FREE
        else CommandCatalog.specFor(command)?.version
}
