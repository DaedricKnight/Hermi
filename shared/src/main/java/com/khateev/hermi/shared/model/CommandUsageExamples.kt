package com.khateev.hermi.shared.model

/**
 * Примеры использования системы версионирования команд
 */
object CommandUsageExamples {

    /**
     * Пример 1: Проверка доступности команды перед выполнением
     */
    fun example1_CheckCommandAvailability() {
        val command = Command.GetSpeed
        val isPro = false  // Бесплатный пользователь

        if (CommandAccessControl.isCommandAvailable(command, isPro)) {
            println("Команда доступна, выполняем...")
            // executeCommand(command)
        } else {
            println("Команда недоступна. Требуется Pro версия")
            // showUpgradeDialog()
        }
    }

    /**
     * Пример 2: Использование Validator с обработкой исключений
     */
    fun example2_ValidateWithException() {
        val command = Command.CheckEngine
        val isPro = false

        try {
            CommandValidator.validateCommandAccess(command, isPro)
            println("Команда доступна, выполняем...")
            // executeCommand(command)
        } catch (e: CommandNotAvailableException) {
            if (e.requiresPro) {
                println("${e.message} - показываем диалог обновления")
                // showUpgradeToProDialog()
            } else {
                println("Команда недоступна")
            }
        }
    }

    /**
     * Пример 3: Использование extension функций
     */
    fun example3_UsingExtensions() {
        val command = Command.FindFood
        val isPro = false

        // Проверка доступности
        if (command.isAvailable(isPro)) {
            println("Команда доступна")
            // executeCommand(command)
        }

        // Получение информации
        val info = command.getInfo()
        println("Команда: ${info.name}, Требует Pro: ${info.requiresPro}")

        // Проверка требования Pro
        if (command.requiresPro()) {
            println("Это Pro команда")
        } else {
            println("Это Free команда")
        }
    }

    /**
     * Пример 4: Проверка всех доступных команд для пользователя
     */
    fun example4_GetAvailableCommands() {
        val isPro = false  // Бесплатный пользователь

        val freeCommands = CommandAccessControl.getAvailableCommands(isPro = false)
        println("Free команды: $freeCommands")

        val proCommands = CommandAccessControl.getAvailableCommands(isPro = true)
        println("Pro команды: $proCommands")
    }

    /**
     * Пример 5: Безопасная проверка без исключений
     */
    fun example5_SafeCheck() {
        val command = Command.LockDoors
        val isPro = false

        val result = CommandValidator.checkCommandAccess(command, isPro)

        when (result) {
            CommandAccessResult.Available -> {
                println("Команда доступна")
                // executeCommand(command)
            }
            CommandAccessResult.RequiresPro -> {
                println("Требуется Pro версия")
                // showUpgradeDialog()
            }
            CommandAccessResult.Unavailable -> {
                println("Команда недоступна")
            }
        }
    }

    /**
     * Пример 6: Использование с VersionManager
     */
    suspend fun example6_WithVersionManager(versionManager: com.khateev.hermi.shared.interfaces.VersionManager) {
        val command = Command.GetOdometer
        val isPro = versionManager.isProNow()

        if (CommandAccessControl.isCommandAvailable(command, isPro)) {
            println("Команда доступна для пользователя")
            // executeCommand(command)
        } else {
            println("Требуется активировать Pro версию")
            // Активируем Pro версию на месяц (30 дней)
            if (versionManager is com.khateev.hermi.shared.manager.VersionManagerImpl) {
                versionManager.activateProSubscription(30 * 24 * 60 * 60 * 1000L)
                println("Pro версия активирована")
            }
        }
    }

    /**
     * Пример 7: Обработка результатов команды с учетом версионирования
     */
    data class CommandExecutionContext(
        val isPro: Boolean
    )

    fun example7_ExecuteWithVersionCheck(command: Command, context: CommandExecutionContext) {
        // Сначала проверяем доступность
        if (!CommandAccessControl.isCommandAvailable(command, context.isPro)) {
            // Команда недоступна
            val commandInfo = CommandValidator.getCommandInfo(command)
            println("Команда '${commandInfo.name}' недоступна в текущей версии")
            if (commandInfo.requiresPro) {
                println("Требуется Pro версия")
                // showUpgradeDialog()
            }
            return
        }

        // Команда доступна, выполняем
        println("Выполняем команду: ${CommandValidator.getCommandInfo(command).name}")
        // executeCommand(command, context)
    }
}

