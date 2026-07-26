package com.khateev.hermi.shared.model

/**
 * Worked examples of the command versioning API
 */
object CommandUsageExamples {

    /**
     * Example 1: check availability before executing
     */
    fun example1_CheckCommandAvailability() {
        val command = Command.SeatMassageOn
        val isPro = false  // free user

        if (CommandAccessControl.isCommandAvailable(command, isPro)) {
            println("Command available, executing...")
            // executeCommand(command)
        } else {
            println("Command unavailable. Pro version required")
            // showUpgradeDialog()
        }
    }

    /**
     * Example 2: Validator with exception handling
     */
    fun example2_ValidateWithException() {
        val command = Command.ClimateAcOn
        val isPro = false

        try {
            CommandValidator.validateCommandAccess(command, isPro)
            println("Command available, executing...")
            // executeCommand(command)
        } catch (e: CommandNotAvailableException) {
            if (e.requiresPro) {
                println("${e.message} - showing the upgrade dialog")
                // showUpgradeToProDialog()
            } else {
                println("Command unavailable")
            }
        }
    }

    /**
     * Example 3: extension functions
     */
    fun example3_UsingExtensions() {
        val command = Command.MediaPlay
        val isPro = false

        // availability
        if (command.isAvailable(isPro)) {
            println("Command available")
            // executeCommand(command)
        }

        // command info
        val info = command.getInfo()
        println("Command: ${info.name}, requires Pro: ${info.requiresPro}")

        // does it need Pro?
        if (command.requiresPro()) {
            println("This is a Pro command")
        } else {
            println("This is a Free command")
        }
    }

    /**
     * Example 4: list every command available to the user
     */
    fun example4_GetAvailableCommands() {
        val isPro = false  // free user

        val freeCommands = CommandAccessControl.getAvailableCommands(isPro = false)
        println("Free commands: $freeCommands")

        val proCommands = CommandAccessControl.getAvailableCommands(isPro = true)
        println("Pro commands: $proCommands")
    }

    /**
     * Example 5: safe check, no exceptions
     */
    fun example5_SafeCheck() {
        val command = Command.ClimateAcOn
        val isPro = false

        val result = CommandValidator.checkCommandAccess(command, isPro)

        when (result) {
            CommandAccessResult.Available -> {
                println("Command available")
                // executeCommand(command)
            }
            CommandAccessResult.RequiresPro -> {
                println("Pro version required")
                // showUpgradeDialog()
            }
            CommandAccessResult.Unavailable -> {
                println("Command unavailable")
            }
        }
    }

    /**
     * Example 6: together with VersionManager
     */
    suspend fun example6_WithVersionManager(versionManager: com.khateev.hermi.shared.interfaces.VersionManager) {
        val command = Command.BodyTrunkOpen
        val isPro = versionManager.isProNow()

        if (CommandAccessControl.isCommandAvailable(command, isPro)) {
            println("Command available to this user")
            // executeCommand(command)
        } else {
            println("Pro version needs activating")
            // activate Pro for a month (30 days)
            if (versionManager is com.khateev.hermi.shared.manager.VersionManagerImpl) {
                versionManager.activateProSubscription(30 * 24 * 60 * 60 * 1000L)
                println("Pro version activated")
            }
        }
    }

    /**
     * Example 7: executing a command with the version check in place
     */
    data class CommandExecutionContext(
        val isPro: Boolean
    )

    fun example7_ExecuteWithVersionCheck(command: Command, context: CommandExecutionContext) {
        // availability first
        if (!CommandAccessControl.isCommandAvailable(command, context.isPro)) {
            // command not available
            val commandInfo = CommandValidator.getCommandInfo(command)
            println("Command '${commandInfo.name}' is not available in the current version")
            if (commandInfo.requiresPro) {
                println("Pro version required")
                // showUpgradeDialog()
            }
            return
        }

        // available — execute it
        println("Executing command: ${CommandValidator.getCommandInfo(command).name}")
        // executeCommand(command, context)
    }
}

