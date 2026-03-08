package com.khateev.hermi.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khateev.hermi.shared.interfaces.VersionManager
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandValidator
import com.khateev.hermi.shared.model.CommandAccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the command list screen
 */
@HiltViewModel
class CommandListViewModel @Inject constructor(
    val versionManager: VersionManager
) : ViewModel() {

    private val TAG = "CommandListVM"

    // Event for command execution
    private val _commandExecutionEvent = MutableSharedFlow<CommandExecutionEvent>()
    val commandExecutionEvent: SharedFlow<CommandExecutionEvent> = _commandExecutionEvent.asSharedFlow()

    /**
     * Command click handler
     */
    fun onCommandClick(command: Command) {
        viewModelScope.launch {
            val isPro = versionManager.isProNow()
            val accessResult = CommandValidator.checkCommandAccess(command, isPro)

            when (accessResult) {
                CommandAccessResult.Available -> {
                    Log.d(TAG, "Command available: $command")
                    _commandExecutionEvent.emit(
                        CommandExecutionEvent.Execute(command)
                    )
                }

                CommandAccessResult.RequiresPro -> {
                    val commandInfo = CommandValidator.getCommandInfo(command)
                    Log.d(TAG, "Pro feature required: ${commandInfo.name}")
                    _commandExecutionEvent.emit(
                        CommandExecutionEvent.ShowProRequiredDialog(commandInfo.name)
                    )
                }

                CommandAccessResult.Unavailable -> {
                    Log.d(TAG, "Command unavailable: $command")
                    _commandExecutionEvent.emit(
                        CommandExecutionEvent.ShowUnavailableDialog
                    )
                }
            }
        }
    }

    /**
     * Upgrade to Pro button click handler
     */
    fun onUpgradeClick() {
        viewModelScope.launch {
            Log.d(TAG, "User clicked upgrade to Pro")
            _commandExecutionEvent.emit(CommandExecutionEvent.ShowUpgradeDialog)
        }
    }
}

/**
 * Events that the ViewModel sends to the UI
 */
sealed class CommandExecutionEvent {
    data class Execute(val command: Command) : CommandExecutionEvent()
    data class ShowProRequiredDialog(val commandName: String) : CommandExecutionEvent()
    object ShowUnavailableDialog : CommandExecutionEvent()
    object ShowUpgradeDialog : CommandExecutionEvent()
}
